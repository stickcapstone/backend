package com.veriweb.veriweb_backend.service.feed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class FeedNewsApiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${veriweb.newsapi.api-key}")
    private String apiKey;

    public FeedNewsApiClient(RestClient.Builder builder, ObjectMapper objectMapper) {
        this.restClient = builder
                .baseUrl("https://newsapi.org/v2")
                .build();
        this.objectMapper = objectMapper;
    }

    public record FeedArticleDto(
            String title,
            String url,
            String source,
            String thumbnailUrl,
            String publishedAt
    ) {}

    public List<FeedArticleDto> fetchTopHeadlines(String newsApiCategory, int pageSize) {
        try {
            String responseBody = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/top-headlines")
                            .queryParam("category", newsApiCategory)
                            .queryParam("language", "en")
                            .queryParam("pageSize", pageSize)
                            .queryParam("apiKey", apiKey)
                            .build())
                    .retrieve()
                    .body(String.class);

            return parseArticles(responseBody);
        } catch (Exception e) {
            log.warn("NewsAPI top-headlines 호출 실패 [category={}]: {}", newsApiCategory, e.getMessage());
            return List.of();
        }
    }

    private List<FeedArticleDto> parseArticles(String responseBody) {
        List<FeedArticleDto> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            for (JsonNode article : root.path("articles")) {
                String title = article.path("title").asText();
                String url = article.path("url").asText();

                if (title.isBlank() || "[Removed]".equals(title) || url.isBlank()) {
                    continue;
                }

                String thumbnailUrl = article.path("urlToImage").asText(null);
                if (thumbnailUrl != null && thumbnailUrl.isBlank()) thumbnailUrl = null;

                result.add(new FeedArticleDto(
                        title,
                        url,
                        article.path("source").path("name").asText("Unknown"),
                        thumbnailUrl,
                        article.path("publishedAt").asText(null)
                ));
            }
        } catch (Exception e) {
            log.warn("NewsAPI 응답 파싱 실패: {}", e.getMessage());
        }
        return result;
    }
}
