package com.veriweb.veriweb_backend.dto.media;

import lombok.Getter;

@Getter
public class ImageAnalysisResponse {

    private final String fileName;
    private final boolean aiGenerated;
    private final double confidence;
    private final String verdict;
    private final String reason;

    public ImageAnalysisResponse(String fileName, boolean aiGenerated, double confidence, String reason) {
        this.fileName = fileName;
        this.aiGenerated = aiGenerated;
        this.confidence = confidence;
        this.reason = reason;
        this.verdict = buildVerdict(aiGenerated, confidence);
    }

    private static String buildVerdict(boolean aiGenerated, double confidence) {
        if (aiGenerated) {
            return confidence >= 70 ? "AI 생성으로 판단됨" : "AI 생성 가능성 있음";
        } else {
            return confidence >= 70 ? "실제 이미지로 판단됨" : "판단 불명확";
        }
    }
}
