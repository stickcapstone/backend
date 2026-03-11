package com.veriweb.veriweb_backend.dto.feed;

import com.veriweb.veriweb_backend.entity.feed.FeedArticle;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FeedArticleResponse {

    private final Long id;
    private final String title;
    private final String url;
    private final String source;
    private final String thumbnailUrl;
    private final String category;
    private final int trustScore;
    private final String grade;
    private final LocalDateTime publishedAt;
    private final LocalDateTime createdAt;

    public static FeedArticleResponse from(FeedArticle article) {
        return new FeedArticleResponse(article);
    }

    private FeedArticleResponse(FeedArticle a) {
        this.id = a.getId();
        this.title = a.getTitle();
        this.url = a.getUrl();
        this.source = a.getSource();
        this.thumbnailUrl = a.getThumbnailUrl();
        this.category = a.getCategory().name();
        this.trustScore = a.getTrustScore();
        this.grade = a.getGrade().name();
        this.publishedAt = a.getPublishedAt();
        this.createdAt = a.getCreatedAt();
    }
}
