package com.veriweb.veriweb_backend.entity.feed;

import com.veriweb.veriweb_backend.entity.analysis.Grade;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "feed_article",
        uniqueConstraints = @UniqueConstraint(columnNames = "url")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false, length = 255)
    private String source;

    @Column(length = 2048)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ArticleCategory category;

    @Column(nullable = false)
    private int trustScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Grade grade;

    @Column(nullable = false)
    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public FeedArticle(String title, String url, String source, String thumbnailUrl,
                       ArticleCategory category, int trustScore, LocalDateTime publishedAt) {
        this.title = title;
        this.url = url;
        this.source = source;
        this.thumbnailUrl = thumbnailUrl;
        this.category = category;
        this.trustScore = trustScore;
        this.grade = Grade.from(trustScore);
        this.publishedAt = publishedAt;
        this.createdAt = LocalDateTime.now();
    }
}
