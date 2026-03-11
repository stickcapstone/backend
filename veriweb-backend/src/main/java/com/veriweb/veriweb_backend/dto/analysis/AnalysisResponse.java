package com.veriweb.veriweb_backend.dto.analysis;

import com.veriweb.veriweb_backend.entity.analysis.Analysis;
import com.veriweb.veriweb_backend.entity.analysis.AnalysisScore;
import com.veriweb.veriweb_backend.entity.analysis.RecommendedArticle;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class AnalysisResponse {

    private final Long analysisId;
    private final String url;
    private final int totalScore;
    private final String grade;
    private final String summary;
    private final List<BreakdownItem> breakdown;
    private final List<ArticleItem> recommendedArticles;
    private final LocalDateTime createdAt;

    public static AnalysisResponse from(Analysis analysis) {
        return new AnalysisResponse(analysis);
    }

    private AnalysisResponse(Analysis analysis) {
        this.analysisId = analysis.getId();
        this.url = analysis.getUrl();
        this.totalScore = analysis.getTotalScore();
        this.grade = analysis.getGrade().name();
        this.summary = analysis.getSummary();
        this.breakdown = analysis.getScores().stream()
                .map(BreakdownItem::from)
                .toList();
        this.recommendedArticles = analysis.getRecommendedArticles().stream()
                .map(ArticleItem::from)
                .toList();
        this.createdAt = analysis.getCreatedAt();
    }

    @Getter
    public static class BreakdownItem {
        private final String category;
        private final int score;
        private final int maxScore;
        private final String reason;

        public static BreakdownItem from(AnalysisScore score) {
            return new BreakdownItem(score);
        }

        private BreakdownItem(AnalysisScore s) {
            this.category = s.getCategory().name();
            this.score = s.getScore();
            this.maxScore = s.getMaxScore();
            this.reason = s.getReason();
        }
    }

    @Getter
    public static class ArticleItem {
        private final String title;
        private final String url;
        private final String source;
        private final LocalDateTime publishedAt;

        public static ArticleItem from(RecommendedArticle article) {
            return new ArticleItem(article);
        }

        private ArticleItem(RecommendedArticle a) {
            this.title = a.getTitle();
            this.url = a.getUrl();
            this.source = a.getSource();
            this.publishedAt = a.getPublishedAt();
        }
    }
}
