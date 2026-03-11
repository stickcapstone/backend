package com.veriweb.veriweb_backend.entity.analysis;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "recommended_article",
        uniqueConstraints = @UniqueConstraint(columnNames = {"analysis_id", "url"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendedArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false, length = 255)
    private String source;

    @Column(nullable = false)
    private LocalDateTime publishedAt;

    @Builder
    public RecommendedArticle(Analysis analysis, String title, String url, String source, LocalDateTime publishedAt) {
        this.analysis = analysis;
        this.title = title;
        this.url = url;
        this.source = source;
        this.publishedAt = publishedAt;
    }
}
