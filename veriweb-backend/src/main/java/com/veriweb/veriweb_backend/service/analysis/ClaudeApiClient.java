package com.veriweb.veriweb_backend.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veriweb.veriweb_backend.common.exception.ErrorCode;
import com.veriweb.veriweb_backend.common.exception.VeriWebException;
import com.veriweb.veriweb_backend.dto.analysis.ClaudeAnalysisResult;
import com.veriweb.veriweb_backend.dto.analysis.CrawledContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class  ClaudeApiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${veriweb.claude.api-key}")
    private String apiKey;

    @Value("${veriweb.claude.model:claude-sonnet-4-6}")
    private String model;

    public ClaudeApiClient(RestClient.Builder builder, ObjectMapper objectMapper) {
        this.restClient = builder
                .baseUrl("https://api.anthropic.com")
                .build();
        this.objectMapper = objectMapper;
    }

    public ClaudeAnalysisResult analyze(CrawledContent content, String url) {
        String prompt = buildPrompt(content, url);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 2048,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );
        try {
            String responseBody = restClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.path("content").get(0).path("text").asText();
            String json = extractJson(text);
            return objectMapper.readValue(json, ClaudeAnalysisResult.class);
        } catch (VeriWebException e) {
            throw e;
        } catch (Exception e) {
            log.error("Claude API 호출 실패: {}", e.getMessage());
            throw new VeriWebException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String buildPrompt(CrawledContent content, String url) {
        String links = content.externalLinks().stream().limit(20)
                .reduce("", (a, b) -> a + "\n- " + b);

        return """
                당신은 전문 팩트체커이자 뉴스 신뢰도 분석가입니다.
                아래 웹 기사를 분석하고 반드시 JSON만 반환하세요. 마크다운, 코드블록, 설명 없이 순수 JSON만.

                URL: %s
                도메인: %s
                제목: %s
                작성자: %s
                수집된 게시일: %s

                본문 내용:
                %s

                외부 링크 목록:%s

                다음 JSON 구조를 정확히 반환하세요:
                {
                  "summary": "한국어로 3~4문장으로 신뢰도 평가 요약",
                  "published_at": "기사 게시일 ISO 8601 형식, 불명확하면 null",
                  "scores": {
                    "DOMAIN": {"score": <0~100 정수>, "reason": "<한국어 평가 이유>"},
                    "AUTHOR": {"score": <0~100 정수>, "reason": "<한국어 평가 이유>"},
                    "REFERENCE": {"score": <0~100 정수>, "reason": "<한국어 평가 이유>"},
                    "CONSISTENCY": {"score": <0~100 정수>, "reason": "<한국어 평가 이유>"},
                    "MANIPULATION": {"score": <0~100 정수>, "reason": "<한국어 평가 이유>"},
                    "ACADEMIC": {"score": <0~100 정수>, "reason": "<한국어 평가 이유>"},
                    "GOV": {"score": <0~100 정수>, "reason": "<한국어 평가 이유>"}
                  }
                }

                각 항목은 독립적으로 0~100점으로 평가하세요. 최종 가중 합산은 서버에서 처리합니다.

                평가 기준:
                - DOMAIN(0~100): 도메인 신뢰도 - 알려진 주요 언론사, .gov/.edu/.ac.kr 등 공신력 있는 도메인일수록 높게
                - AUTHOR(0~100): 작성자/출처 명확성 - 실명, 소속, 기자 정보가 명확할수록 높게
                - REFERENCE(0~100): 근거/인용 충실도 - 외부 링크, 출처 명시, 인용이 많을수록 높게
                - CONSISTENCY(0~100): 정보 일관성 - 제목·본문 일치, 사실 오류 없음, 내부 모순 없을수록 높게
                - MANIPULATION(0~100): 조작/품질 이상 없음 - 조작·클릭베이트·감정조작·과장이 없을수록 높게 (조작 없으면 100, 심각한 조작이면 0)
                - ACADEMIC(0~100): 학술 저널/연구 인용 여부 - 논문, 연구 결과 인용이 많을수록 높게
                - GOV(0~100): 정부/공공기관 자료 인용 - 정부 발표, 공공기관 통계 인용이 있을수록 높게
                """.formatted(
                url,
                content.domain(),
                content.title(),
                content.author(),
                content.publishedAt() != null ? content.publishedAt() : "미확인",
                content.bodyText(),
                links.isBlank() ? " 없음" : links
        );
    }

    public String explainImageAnalysis(byte[] imageBytes, String mimeType, boolean aiGenerated, double confidencePct) {
        String prompt = """
                이 이미지는 AI 생성물 탐지 시스템에 의해 분석되었습니다.
                탐지 결과: %s (신뢰도: %.1f%%)

                위 탐지 결과를 바탕으로, 이 이미지의 특징을 관찰하여 AI 생성 여부 판단 근거를 한국어로 2~3문장으로 설명해주세요.
                결론 판단 없이 근거 설명만 작성하세요.
                """.formatted(aiGenerated ? "AI 생성" : "실제 이미지", confidencePct);

        List<Object> contentList = List.of(
                java.util.Map.of(
                        "type", "image",
                        "source", java.util.Map.of(
                                "type", "base64",
                                "media_type", mimeType,
                                "data", java.util.Base64.getEncoder().encodeToString(imageBytes)
                        )
                ),
                java.util.Map.of("type", "text", "text", prompt)
        );

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 512,
                "messages", List.of(Map.of("role", "user", "content", contentList))
        );

        try {
            String responseBody = restClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("content").get(0).path("text").asText();
        } catch (Exception e) {
            log.warn("Claude 이미지 설명 생성 실패: {}", e.getMessage());
            return aiGenerated
                    ? "AI 생성 패턴이 탐지되었습니다. 세부 근거 분석에 실패했습니다."
                    : "실제 이미지 특성이 확인되었습니다. 세부 근거 분석에 실패했습니다.";
        }
    }

    public String explainVideoAnalysis(int totalFrames, int aiFrames, double confidencePct, boolean aiGenerated) {
        String prompt = """
                영상 AI 생성물 탐지 결과를 분석해주세요.
                - 전체 분석 프레임 수: %d
                - AI 생성으로 감지된 프레임 수: %d
                - 전체 AI 감지 신뢰도: %.1f%%
                - 최종 판정: %s

                위 통계를 바탕으로 AI 생성 여부 판단 근거를 한국어로 2~3문장으로 설명해주세요.
                결론 판단 없이 근거 설명만 작성하세요.
                """.formatted(totalFrames, aiFrames, confidencePct, aiGenerated ? "AI 생성 영상" : "실제 영상");

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 512,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        try {
            String responseBody = restClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("content").get(0).path("text").asText();
        } catch (Exception e) {
            log.warn("Claude 영상 설명 생성 실패: {}", e.getMessage());
            return String.format("전체 %d 프레임 중 %d 프레임에서 AI 생성 패턴이 감지되었습니다.",
                    totalFrames, aiFrames);
        }
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}
