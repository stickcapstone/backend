package com.veriweb.veriweb_backend.entity.analysis;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "analysis")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false)
    private int totalScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Grade grade;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column
    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnalysisScore> scores = new ArrayList<>();

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecommendedArticle> recommendedArticles = new ArrayList<>();

    @Builder
    public Analysis(String url, int totalScore, Grade grade, String summary, LocalDateTime publishedAt) {
        this.url = url;
        this.totalScore = totalScore;
        this.grade = grade;
        this.summary = summary;
        this.publishedAt = publishedAt;
        this.createdAt = LocalDateTime.now();
    }
}
