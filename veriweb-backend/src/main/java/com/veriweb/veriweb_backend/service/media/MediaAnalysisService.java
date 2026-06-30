package com.veriweb.veriweb_backend.service.media;

import com.veriweb.veriweb_backend.common.exception.ErrorCode;
import com.veriweb.veriweb_backend.common.exception.VeriWebException;
import com.veriweb.veriweb_backend.dto.media.ImageAnalysisResponse;
import com.veriweb.veriweb_backend.dto.media.VideoAnalysisResponse;
import com.veriweb.veriweb_backend.service.analysis.ClaudeApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaAnalysisService {

    private static final double AI_THRESHOLD = 50.0;

    private final HiveApiClient hiveApiClient;
    private final ClaudeApiClient claudeApiClient;

    public ImageAnalysisResponse analyzeImage(MultipartFile file) {
        validateImageFile(file);

        HiveApiClient.ImageResult hiveResult = hiveApiClient.analyzeImage(file);
        double confidencePct = Math.round(hiveResult.aiScore() * 1000.0) / 10.0;
        boolean aiGenerated = confidencePct >= AI_THRESHOLD;

        String reason;
        try {
            reason = claudeApiClient.explainImageAnalysis(
                    file.getBytes(), file.getContentType(), aiGenerated, confidencePct);
        } catch (Exception e) {
            log.warn("이미지 파일 읽기 실패, 기본 reason 사용: {}", e.getMessage());
            reason = aiGenerated
                    ? "AI 생성 패턴이 탐지되었습니다."
                    : "실제 이미지 특성이 확인되었습니다.";
        }

        return new ImageAnalysisResponse(
                file.getOriginalFilename(),
                aiGenerated,
                confidencePct,
                reason
        );
    }

    public VideoAnalysisResponse analyzeVideo(MultipartFile file) {
        validateVideoFile(file);

        List<HiveApiClient.VideoFrameResult> frames = hiveApiClient.analyzeVideo(file);

        if (frames.isEmpty()) {
            throw new VeriWebException(ErrorCode.MEDIA_ANALYSIS_FAILED);
        }

        int totalFrames = frames.size();
        int aiFrames = (int) frames.stream()
                .filter(f -> f.aiScore() >= 0.9)
                .count();

        double avgAiScore = frames.stream()
                .mapToDouble(HiveApiClient.VideoFrameResult::aiScore)
                .average()
                .orElse(0.0);
        double confidencePct = Math.round(avgAiScore * 1000.0) / 10.0;
        boolean aiGenerated = confidencePct >= AI_THRESHOLD;

        String reason = claudeApiClient.explainVideoAnalysis(totalFrames, aiFrames, confidencePct, aiGenerated);

        return new VideoAnalysisResponse(
                file.getOriginalFilename(),
                aiGenerated,
                confidencePct,
                reason,
                totalFrames,
                aiFrames
        );
    }

    private static final java.util.Set<String> IMAGE_EXTENSIONS =
            java.util.Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "tiff", "tif");

    private static final java.util.Set<String> VIDEO_EXTENSIONS =
            java.util.Set.of("mp4", "mov", "avi", "mkv", "webm", "flv", "wmv", "m4v");

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new VeriWebException(ErrorCode.MEDIA_FILE_REQUIRED);
        }
        String contentType = file.getContentType();
        boolean validType = contentType != null && contentType.startsWith("image/");
        boolean validExt = hasExtension(file.getOriginalFilename(), IMAGE_EXTENSIONS);
        if (!validType && !validExt) {
            throw new VeriWebException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    private void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new VeriWebException(ErrorCode.MEDIA_FILE_REQUIRED);
        }
        String contentType = file.getContentType();
        boolean validType = contentType != null && contentType.startsWith("video/");
        boolean validExt = hasExtension(file.getOriginalFilename(), VIDEO_EXTENSIONS);
        if (!validType && !validExt) {
            throw new VeriWebException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    private boolean hasExtension(String filename, java.util.Set<String> allowed) {
        if (filename == null) return false;
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return false;
        return allowed.contains(filename.substring(dot + 1).toLowerCase());
    }
}
