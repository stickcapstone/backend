package com.veriweb.veriweb_backend.service.analysis;

import com.veriweb.veriweb_backend.common.exception.ErrorCode;
import com.veriweb.veriweb_backend.common.exception.VeriWebException;
import com.veriweb.veriweb_backend.dto.analysis.AnalysisResponse;
import com.veriweb.veriweb_backend.dto.analysis.ClaudeAnalysisResult;
import com.veriweb.veriweb_backend.dto.analysis.CrawledContent;
import com.veriweb.veriweb_backend.dto.analysis.NewsArticle;
import com.veriweb.veriweb_backend.entity.analysis.Analysis;
import com.veriweb.veriweb_backend.entity.analysis.AnalysisScore;
import com.veriweb.veriweb_backend.entity.analysis.Grade;
import com.veriweb.veriweb_backend.entity.analysis.RecommendedArticle;
import com.veriweb.veriweb_backend.entity.analysis.ScoreCategory;
import com.veriweb.veriweb_backend.repository.analysis.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
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

        // 3. 총점 계산 및 등급 결정
        int totalScore = result.scores().values().stream()
                .mapToInt(ClaudeAnalysisResult.ScoreItem::score)
                .sum();

        // 4. Analysis 엔티티 구성
        Analysis analysis = Analysis.builder()
                .url(url)
                .totalScore(totalScore)
                .grade(Grade.from(totalScore))
                .summary(result.summary())
                .publishedAt(parseDateTime(result.publishedAt()))
                .build();

        // 5. 카테고리별 점수 추가
        result.scores().forEach((categoryName, item) -> {
            AnalysisScore score = AnalysisScore.builder()
                    .analysis(analysis)
                    .category(ScoreCategory.valueOf(categoryName))
                    .score(item.score())
                    .reason(item.reason())
                    .build();
            analysis.getScores().add(score);
        });

        // 6. NewsAPI 추천 기사 추가 (실패해도 분석 결과는 저장)
        List<NewsArticle> newsArticles = newsApiClient.searchRelatedArticles(content.title(), content.domain());
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

        return analysisRepository.save(analysis);
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
