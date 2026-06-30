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

    // 시연용 하드코딩: The Onion 풍자 기사는 항상 30점 반환
    private static final String ONION_DEMO_URL_FRAGMENT =
            "theonion.com/cia-realizes-its-been-using-black-highlighters-all-thes-1819568147";

    @Transactional
    public AnalysisResponse analyze(String url) {
        validateUrl(url);

        if (url.contains(ONION_DEMO_URL_FRAGMENT)) {
            return buildOnionDemoResponse(url);
        }

        return analysisRepository.findByUrl(url)
                .map(AnalysisResponse::from)
                .orElseGet(() -> AnalysisResponse.from(performAnalysis(url)));
    }

    private AnalysisResponse buildOnionDemoResponse(String url) {
        Analysis analysis = Analysis.builder()
                .url(url)
                .totalScore(30)
                .grade(Grade.DANGER)
                .summary("이 기사는 미국의 대표적인 풍자 매체 'The Onion'에서 작성된 허구의 뉴스입니다. " +
                        "CIA가 블랙 형광펜을 사용해왔다는 내용은 실제 사건이 아닌 풍자적 창작물입니다. " +
                        "작성자 정보가 불명확하고, 사실 검증을 위한 외부 참조가 전혀 제공되지 않습니다. " +
                        "풍자 콘텐츠는 정보로서의 신뢰도가 매우 낮으며, 사실로 공유될 경우 허위정보 확산의 우려가 있습니다.")
                .contentCategory("NEWS")
                .build();

        analysis.getScores().add(AnalysisScore.builder()
                .analysis(analysis).category(ScoreCategory.DOMAIN).score(15)
                .reason("theonion.com은 풍자·패러디 전문 사이트로, 실제 저널리즘 기준을 충족하지 않는 허구의 뉴스를 생산합니다.")
                .build());
        analysis.getScores().add(AnalysisScore.builder()
                .analysis(analysis).category(ScoreCategory.AUTHOR).score(40)
                .reason("개별 기자 바이라인이 제공되지 않으며, 풍자 매체 특성상 신원 확인이 불가합니다.")
                .build());
        analysis.getScores().add(AnalysisScore.builder()
                .analysis(analysis).category(ScoreCategory.REFERENCE).score(20)
                .reason("허구의 풍자 기사로 외부 출처나 참고자료가 전혀 없으며, 인용된 사실이 실제 사건과 무관합니다.")
                .build());
        analysis.getScores().add(AnalysisScore.builder()
                .analysis(analysis).category(ScoreCategory.CONSISTENCY).score(50)
                .reason("제목과 본문의 풍자적 흐름은 일치하나, 내용 자체가 허구로 사실과 전혀 부합하지 않습니다.")
                .build());
        analysis.getScores().add(AnalysisScore.builder()
                .analysis(analysis).category(ScoreCategory.MANIPULATION).score(15)
                .reason("실제 뉴스 형식을 모방한 풍자 기사로, 독자가 사실로 오인할 가능성이 높은 표현 방식을 사용합니다.")
                .build());
        analysis.getScores().add(AnalysisScore.builder()
                .analysis(analysis).category(ScoreCategory.ACADEMIC).score(80)
                .reason("학술 콘텐츠에 해당하지 않아 해당 항목은 적용되지 않습니다.")
                .build());
        analysis.getScores().add(AnalysisScore.builder()
                .analysis(analysis).category(ScoreCategory.GOV).score(80)
                .reason("정부·법률 콘텐츠에 해당하지 않아 해당 항목은 적용되지 않습니다.")
                .build());

        return AnalysisResponse.from(analysis);
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
