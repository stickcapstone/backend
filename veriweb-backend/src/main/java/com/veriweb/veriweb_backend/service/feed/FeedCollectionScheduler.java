package com.veriweb.veriweb_backend.service.feed;

import com.veriweb.veriweb_backend.entity.feed.ArticleCategory;
import com.veriweb.veriweb_backend.entity.feed.FeedArticle;
import com.veriweb.veriweb_backend.repository.feed.FeedArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedCollectionScheduler {

    private static final int MIN_TRUST_SCORE = 60;
    private static final int PAGE_SIZE = 30;

    // 주요 언론사 목록 — 해당 소스는 85점 이상으로 평가
    private static final Set<String> HIGH_TRUST_SOURCES = Set.of(
            "Reuters", "Associated Press", "BBC News", "BBC",
            "The Guardian", "Bloomberg", "Financial Times",
            "The New York Times", "The Washington Post", "Wall Street Journal",
            "ABC News", "NBC News", "CBS News", "NPR", "CNN",
            "연합뉴스", "Yonhap", "KBS", "MBC", "SBS"
    );

    // VeriWeb ArticleCategory → NewsAPI category 매핑
    private static final Map<ArticleCategory, String> CATEGORY_MAP = Map.of(
            ArticleCategory.POLITICS, "general",
            ArticleCategory.ECONOMY, "business",
            ArticleCategory.IT, "technology",
            ArticleCategory.HEALTH, "health"
    );

    private final FeedArticleRepository feedArticleRepository;
    private final FeedNewsApiClient feedNewsApiClient;

    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void collectFeedArticles() {
        log.info("피드 기사 자동 수집 시작");
        int saved = 0;

        for (Map.Entry<ArticleCategory, String> entry : CATEGORY_MAP.entrySet()) {
            ArticleCategory category = entry.getKey();
            String newsApiCategory = entry.getValue();

            var articles = feedNewsApiClient.fetchTopHeadlines(newsApiCategory, PAGE_SIZE);
            for (FeedNewsApiClient.FeedArticleDto dto : articles) {
                if (feedArticleRepository.existsByUrl(dto.url())) {
                    continue;
                }

                int trustScore = calculateTrustScore(dto.source());
                if (trustScore < MIN_TRUST_SCORE) {
                    continue;
                }

                LocalDateTime publishedAt = parseDateTime(dto.publishedAt());
                if (publishedAt == null) {
                    publishedAt = LocalDateTime.now();
                }

                FeedArticle article = FeedArticle.builder()
                        .title(dto.title())
                        .url(dto.url())
                        .source(dto.source())
                        .thumbnailUrl(dto.thumbnailUrl())
                        .category(category)
                        .trustScore(trustScore)
                        .publishedAt(publishedAt)
                        .build();

                feedArticleRepository.save(article);
                saved++;
            }
        }

        log.info("피드 기사 자동 수집 완료: {}건 저장", saved);
    }

    private int calculateTrustScore(String source) {
        if (source == null) return 65;
        for (String trusted : HIGH_TRUST_SOURCES) {
            if (source.contains(trusted)) return 88;
        }
        // top-headlines는 NewsAPI가 1차 검증한 소스이므로 기본 70점
        return 70;
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
