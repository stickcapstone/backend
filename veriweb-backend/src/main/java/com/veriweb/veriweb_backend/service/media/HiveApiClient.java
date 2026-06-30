package com.veriweb.veriweb_backend.service.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veriweb.veriweb_backend.common.exception.ErrorCode;
import com.veriweb.veriweb_backend.common.exception.VeriWebException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class HiveApiClient {

    private static final String HIVE_BASE_URL = "https://api.thehive.ai";
    private static final String SYNC_ENDPOINT = "/api/v3/hive/ai-generated-and-deepfake-content-detection";
    private static final String AI_GENERATED_CLASS = "ai_generated";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${veriweb.hive.api-key:}")
    private String apiKey;

    public HiveApiClient(RestClient.Builder builder, ObjectMapper objectMapper) {
        this.restClient = builder
                .baseUrl(HIVE_BASE_URL)
                .build();
        this.objectMapper = objectMapper;
    }

    public record ImageResult(double aiScore) {}

    public record VideoFrameResult(double aiScore) {}

    public ImageResult analyzeImage(MultipartFile file) {
        String responseBody = callHiveApi(file, "media");
        return parseImageResponse(responseBody);
    }

    public List<VideoFrameResult> analyzeVideo(MultipartFile file) {
        String responseBody = callHiveApi(file, "media");
        return parseVideoResponse(responseBody);
    }

    private String callHiveApi(MultipartFile file, String fieldName) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new VeriWebException(ErrorCode.HIVE_API_KEY_MISSING);
        }

        try {
            byte[] bytes = file.getBytes();
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : fieldName;

            ByteArrayResource resource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() { return filename; }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add(fieldName, resource);

            return restClient.post()
                    .uri(SYNC_ENDPOINT)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (VeriWebException e) {
            throw e;
        } catch (Exception e) {
            log.error("Hive API 호출 실패: {}", e.getMessage());
            throw new VeriWebException(ErrorCode.MEDIA_ANALYSIS_FAILED);
        }
    }

    private ImageResult parseImageResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode classes = root.path("output").get(0).path("classes");
            double aiScore = extractClassScore(classes, AI_GENERATED_CLASS);
            return new ImageResult(aiScore);
        } catch (Exception e) {
            log.error("Hive 이미지 응답 파싱 실패: {}", e.getMessage());
            throw new VeriWebException(ErrorCode.MEDIA_ANALYSIS_FAILED);
        }
    }

    private List<VideoFrameResult> parseVideoResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode outputs = root.path("output");
            List<VideoFrameResult> frames = new ArrayList<>();
            for (JsonNode output : outputs) {
                JsonNode classes = output.path("classes");
                double aiScore = extractClassScore(classes, AI_GENERATED_CLASS);
                frames.add(new VideoFrameResult(aiScore));
            }
            return frames;
        } catch (Exception e) {
            log.error("Hive 비디오 응답 파싱 실패: {}", e.getMessage());
            throw new VeriWebException(ErrorCode.MEDIA_ANALYSIS_FAILED);
        }
    }

    private double extractClassScore(JsonNode classes, String targetClass) {
        for (JsonNode cls : classes) {
            if (targetClass.equals(cls.path("class").asText())) {
                return cls.path("value").asDouble(0.0);
            }
        }
        return 0.0;
    }
}
