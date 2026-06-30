package com.veriweb.veriweb_backend.dto.media;

import lombok.Getter;

@Getter
public class VideoAnalysisResponse {

    private final String fileName;
    private final boolean aiGenerated;
    private final double confidence;
    private final String verdict;
    private final String reason;
    private final int totalFrames;
    private final int aiFrames;

    public VideoAnalysisResponse(String fileName, boolean aiGenerated, double confidence,
                                  String reason, int totalFrames, int aiFrames) {
        this.fileName = fileName;
        this.aiGenerated = aiGenerated;
        this.confidence = confidence;
        this.reason = reason;
        this.totalFrames = totalFrames;
        this.aiFrames = aiFrames;
        this.verdict = buildVerdict(aiGenerated, confidence);
    }

    private static String buildVerdict(boolean aiGenerated, double confidence) {
        if (aiGenerated) {
            return confidence >= 70 ? "AI 생성 영상으로 판단됨" : "AI 생성 가능성 있음";
        } else {
            return (100 - confidence) >= 70 ? "실제 영상으로 판단됨" : "판단 불명확";
        }
    }
}
