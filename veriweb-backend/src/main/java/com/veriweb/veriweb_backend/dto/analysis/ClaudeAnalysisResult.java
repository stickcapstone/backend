package com.veriweb.veriweb_backend.dto.analysis;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record ClaudeAnalysisResult(
        String summary,
        @JsonProperty("published_at") String publishedAt,
        Map<String, ScoreItem> scores
) {
    public record ScoreItem(int score, String reason) {}
}
