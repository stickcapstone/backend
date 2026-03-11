package com.veriweb.veriweb_backend.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veriweb.veriweb_backend.dto.analysis.NewsArticle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class NewsApiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${veriweb.newsapi.api-key}")
    private String apiKey;

    public NewsApiClient(RestClient.Builder builder, ObjectMapper objectMapper) {
        this.restClient = builder
                .baseUrl("https://newsapi.org/v2")
                .build();
        this.objectMapper = objectMapper;
    }

    public List<NewsArticle> searchRelatedArticles(String title, String excludeDomain) {
        String keyword = title.length() > 50 ? title.substring(0, 50) : title;

        try {
            String responseBody = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/everything")
                            .queryParam("q", keyword)
                            .queryParam("sortBy", "relevancy")
                            .queryParam("pageSize", "30")
                            .queryParam("apiKey", apiKey)
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode articles = root.path("articles");

            List<NewsArticle> result = new ArrayList<>();
            for (JsonNode article : articles) {
                String url = article.path("url").asText();
                String articleTitle = article.path("title").asText();

                if (url.contains(excludeDomain) || articleTitle.isBlank() || "[Removed]".equals(articleTitle)) {
                    continue;
                }

                result.add(new NewsArticle(
                        articleTitle,
                        url,
                        article.path("source").path("name").asText(),
                        article.path("publishedAt").asText()
                ));
            }
            return result;
        } catch (Exception e) {
            log.warn("NewsAPI 호출 실패, 추천 기사 없이 진행: {}", e.getMessage());
            return List.of();
        }
    }
}
