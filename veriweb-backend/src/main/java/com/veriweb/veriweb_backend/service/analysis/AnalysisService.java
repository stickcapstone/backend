package com.veriweb.veriweb_backend.service.analysis;

import com.veriweb.veriweb_backend.common.exception.ErrorCode;
import com.veriweb.veriweb_backend.common.exception.VeriWebException;
import com.veriweb.veriweb_backend.dto.analysis.AnalysisResponse;
import com.veriweb.veriweb_backend.dto.analysis.ClaudeAnalysisResult;
import com.veriweb.veriweb_backend.dto.analysis.CrawledContent;
import com.veriweb.veriweb_backend.dto.analysis.NewsArticle;
import com.veriweb.veriweb_backend.entity.analysis.Analysis;
import com.veriweb.veriweb_backend.entity.analysis.AnalysisScore;
import com.veriweb.veriweb_backend.entity.analysis.ContentCategory;
import com.veriweb.veriweb_backend.entity.analysis.Grade;
import com.veriweb.veriweb_backend.entity.analysis.RecommendedArticle;
import com.veriweb.veriweb_backend.entity.analysis.ScoreCategory;
import com.veriweb.veriweb_backend.entity.feed.ArticleCategory;
import com.veriweb.veriweb_backend.entity.feed.FeedArticle;
import com.veriweb.veriweb_backend.repository.analysis.AnalysisRepository;
import com.veriweb.veriweb_backend.repository.feed.FeedArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private static final int FEED_MIN_SCORE = 60;

    private final AnalysisRepository analysisRepository;
    private final FeedArticleRepository feedArticleRepository;
    private final CrawlerService crawlerService;
    private final ClaudeApiClient claudeApiClient;
    private final NewsApiClient newsApiClient;

    @Transactional
    public AnalysisResponse analyze(String url) {
        validateUrl(url);

        return analysisRepository.findByUrl(url)
                .map(AnalysisResponse::from)
                .orElseGet(() -> AnalysisResponse.from(performAnalysis(url)));
    }

    @Transactional(readOnly = true)
    public AnalysisResponse getById(Long analysisId) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new VeriWebException(ErrorCode.ANALYSIS_NOT_FOUND));
        return AnalysisResponse.from(analysis);
    }

    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new VeriWebException(ErrorCode.MISSING_URL);
        }
        try {
            URL parsed = new URL(url);
            if (!parsed.getProtocol().equals("http") && !parsed.getProtocol().equals("https")) {
                throw new VeriWebException(ErrorCode.INVALID_URL);
            }
        } catch (MalformedURLException e) {
            throw new VeriWebException(ErrorCode.INVALID_URL);
        }
    }

    private Analysis performAnalysis(String url) {
        // 1. 크롤링
        CrawledContent content = crawlerService.crawl(url);

        // 2. Claude API 분석
        ClaudeAnalysisResult result = claudeApiClient.analyze(content, url);

        // 3. 항목별 원점수(0~100) 수집
        record ScoreEntry(int rawScore, String reason) {}
        java.util.Map<ScoreCategory, ScoreEntry> entries = new java.util.EnumMap<>(ScoreCategory.class);
        java.util.Map<ScoreCategory, Integer> rawScores = new java.util.EnumMap<>(ScoreCategory.class);

        for (ScoreCategory sc : ScoreCategory.values()) {
            ClaudeAnalysisResult.ScoreItem item = result.scores() != null
                    ? result.scores().get(sc.name()) : null;
            int rawScore = (item != null) ? Math.min(Math.max(item.score(), 0), 100) : 0;
            String reason = (item != null && item.reason() != null) ? item.reason() : "분석 불가";
            entries.put(sc, new ScoreEntry(rawScore, reason));
            rawScores.put(sc, rawScore);
        }

        // 4. 콘텐츠 카테고리별 가중치로 총점 계산 (카테고리별 상한 포함)
        ContentCategory contentCategory = ContentCategory.fromString(result.category());
        int totalScore = contentCategory.calculateTotalScore(rawScores);

        // 5. NewsAPI 추천 기사 수집
        List<NewsArticle> newsArticles = newsApiClient.searchRelatedArticles(content.title(), content.domain());

        // 6. 수집 기사 부족 시 감점 (0개: -10점, 1~4개: -5점)
        int articleCount = newsArticles.size();
        if (articleCount == 0) {
            totalScore = Math.max(0, totalScore - 10);
        } else if (articleCount < 5) {
            totalScore = Math.max(0, totalScore - 5);
        }

        // 7. Analysis 엔티티 구성
        Analysis analysis = Analysis.builder()
                .url(url)
                .totalScore(totalScore)
                .grade(Grade.from(totalScore))
                .summary(result.summary())
                .contentCategory(contentCategory.name())
                .publishedAt(parseDateTime(result.publishedAt()))
                .build();

        // 8. 항목별 점수 추가
        for (ScoreCategory sc : ScoreCategory.values()) {
            ScoreEntry entry = entries.get(sc);
            analysis.getScores().add(AnalysisScore.builder()
                    .analysis(analysis)
                    .category(sc)
                    .score(entry.rawScore())
                    .reason(entry.reason())
                    .build());
        }

        // 9. 추천 기사 추가 (상위 15개)
        newsArticles.stream().limit(15).forEach(article -> {
            LocalDateTime publishedAt = parseDateTime(article.publishedAt());
            RecommendedArticle rec = RecommendedArticle.builder()
                    .analysis(analysis)
                    .title(article.title())
                    .url(article.url())
                    .source(article.source())
                    .publishedAt(publishedAt != null ? publishedAt : LocalDateTime.now())
                    .build();
            analysis.getRecommendedArticles().add(rec);
        });

        Analysis saved = analysisRepository.save(analysis);
        saveFeedArticleIfEligible(saved, content);
        return saved;
    }

    private void saveFeedArticleIfEligible(Analysis analysis, CrawledContent content) {
        if (analysis.getTotalScore() < FEED_MIN_SCORE) return;
        if (feedArticleRepository.existsByUrl(analysis.getUrl())) return;

        try {
            ArticleCategory articleCategory = mapToArticleCategory(
                    ContentCategory.fromString(analysis.getContentCategory()));
            LocalDateTime publishedAt = analysis.getPublishedAt() != null
                    ? analysis.getPublishedAt() : analysis.getCreatedAt();

            FeedArticle feedArticle = FeedArticle.builder()
                    .title(content.title())
                    .url(analysis.getUrl())
                    .source(content.domain())
                    .thumbnailUrl(content.thumbnailUrl())
                    .category(articleCategory)
                    .trustScore(analysis.getTotalScore())
                    .publishedAt(publishedAt)
                    .build();

            feedArticleRepository.save(feedArticle);
            log.info("피드 기사 자동 저장: score={}, url={}", analysis.getTotalScore(), analysis.getUrl());
        } catch (Exception e) {
            log.warn("피드 기사 저장 실패 (무시됨): {}", e.getMessage());
        }
    }

    private static ArticleCategory mapToArticleCategory(ContentCategory category) {
        return switch (category) {
            case POLITICS, GOVERNMENT, LEGAL, PETITION, COMMUNITY, SNS,
                 RELIGION, NEWS, NEWSLETTER, FACTCHECK, SPORTS, TRAVEL, FOOD, MARKETING -> ArticleCategory.POLITICS;
            case FINANCE, REAL_ESTATE, CORPORATE, STATISTICS -> ArticleCategory.ECONOMY;
            case TECH_BLOG, SCIENCE, WIKI, EDUCATION, REVIEW, ACADEMIC_PAPER -> ArticleCategory.IT;
            case HEALTH -> ArticleCategory.HEALTH;
        };
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return OffsetDateTime.parse(dateStr).toLocalDateTime();
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateStr);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
