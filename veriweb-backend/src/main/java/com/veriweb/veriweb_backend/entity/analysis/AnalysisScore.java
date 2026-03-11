package com.veriweb.veriweb_backend.entity.analysis;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "analysis_score")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ScoreCategory category;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int maxScore;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Builder
    public AnalysisScore(Analysis analysis, ScoreCategory category, int score, String reason) {
        this.analysis = analysis;
        this.category = category;
        this.score = score;
        this.maxScore = category.getMaxScore();
        this.reason = reason;
    }
}
