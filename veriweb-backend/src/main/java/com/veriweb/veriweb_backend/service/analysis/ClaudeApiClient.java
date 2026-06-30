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
                당신은 웹 콘텐츠 신뢰도 분석가입니다.
                아래 웹 페이지를 분석하고 반드시 JSON만 반환하세요. 마크다운, 코드블록, 설명 없이 순수 JSON만.

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
                  "category": "<아래 목록 중 정확히 하나>",
                  "summary": "한국어로 3~4문장으로 신뢰도 평가 요약",
                  "published_at": "기사 게시일 ISO 8601 형식, 불명확하면 null",
                  "scores": {
                    "DOMAIN": {"score": <0~100 정수>, "reason": "<한국어 감점 이유, 감점 없으면 '문제 없음'>"},
                    "AUTHOR": {"score": <0~100 정수>, "reason": "<한국어 감점 이유, 감점 없으면 '문제 없음'>"},
                    "REFERENCE": {"score": <0~100 정수>, "reason": "<한국어 감점 이유, 감점 없으면 '문제 없음'>"},
                    "CONSISTENCY": {"score": <0~100 정수>, "reason": "<한국어 감점 이유, 감점 없으면 '문제 없음'>"},
                    "MANIPULATION": {"score": <0~100 정수>, "reason": "<한국어 감점 이유, 감점 없으면 '문제 없음'>"},
                    "ACADEMIC": {"score": <0~100 정수>, "reason": "<한국어 감점 이유, 감점 없으면 '문제 없음'>"},
                    "GOV": {"score": <0~100 정수>, "reason": "<한국어 감점 이유, 감점 없으면 '문제 없음'>"}
                  }
                }

                category 선택 목록 (반드시 아래 영문 코드 중 하나만):
                NEWS(뉴스/언론), TECH_BLOG(개발/기술 블로그), ACADEMIC_PAPER(학술 논문/리포트),
                GOVERNMENT(정부/공공기관), COMMUNITY(커뮤니티/포럼), SNS(SNS/개인 미디어),
                HEALTH(의료/건강), LEGAL(법률/법령), FINANCE(금융/경제), EDUCATION(교육/학습),
                WIKI(위키/백과사전), FACTCHECK(팩트체크 전문), STATISTICS(통계/데이터 리포트),
                NEWSLETTER(뉴스레터), SCIENCE(과학/환경), REAL_ESTATE(부동산/세금),
                SPORTS(스포츠/엔터테인먼트), TRAVEL(여행/지역), FOOD(음식/레시피),
                RELIGION(종교/철학), CORPORATE(기업 보도자료/IR), REVIEW(제품 리뷰/비교),
                MARKETING(마케팅 콘텐츠), PETITION(청원/캠페인), POLITICS(정치/선거)

                === 채점 방식: 100점 감점제 ===
                모든 항목은 100점에서 시작합니다. 명백하고 구체적인 문제가 확인될 때만 감점하세요.
                불확실하거나 판단이 애매한 경우에는 감점하지 마세요. 일반적인 블로그·커뮤니티·개인 콘텐츠는 그 특성상 부족한 면이 있어도 감점을 최소화하세요.

                항목별 감점 기준 (score = 100 - 감점량):

                - DOMAIN: 기본 100점. 아래 경우에만 감점.
                  · 알려진 스팸·피싱·가짜뉴스 사이트: -40~60
                  · HTTP(비암호화) 사용: -10
                  · 일반 블로그·커뮤니티·개인 사이트: 감점 없음 (0~-5)
                  · HTTPS 사용하는 일반 사이트: 감점 없음

                - AUTHOR: 기본 100점. 아래 경우에만 감점.
                  · 익명이지만 일반적인 포럼/커뮤니티/SNS 글: 감점 없음
                  · 작성자 정보가 전혀 없는 뉴스/언론 기사: -20~30
                  · 허위 또는 가상 인물 명의 확인 시: -40~60

                - REFERENCE: 기본 100점. 아래 경우에만 감점.
                  · 참고자료 없이 의학·법률·금융 등 전문 주장을 사실처럼 단정: -20~40
                  · 링크 없이 주장만 있는 뉴스 기사: -10~20
                  · 레시피·여행·일상·오피니언 등 출처가 불필요한 콘텐츠: 감점 없음

                - CONSISTENCY: 기본 100점. 아래 경우에만 감점.
                  · 제목과 본문 내용이 명백히 다른 경우: -20~40
                  · 본문 내에서 서로 모순되는 사실 주장이 명백히 존재: -15~30
                  · 일반적인 의견 차이나 관점 차이: 감점 없음

                - MANIPULATION: 기본 100점. 아래 경우에만 감점.
                  · 명백한 클릭베이트 제목 (사실과 다른 과장): -15~30
                  · 공포·혐오·분노를 의도적으로 유발하는 표현 과다: -10~25
                  · 허위사실을 사실인 것처럼 주장: -30~50
                  · 감정적 표현이 있어도 정보가 정확하면: 감점 없음
                  · 광고성이나 홍보성 내용이 있어도 명시적이면: 감점 없음 (MARKETING 카테고리로 분류)

                - ACADEMIC: 기본 100점. 학술·의료·과학 콘텐츠가 아닌 경우 거의 감점하지 마세요.
                  · 의료·과학 콘텐츠에서 근거 논문 없이 치료 효과 주장: -20~40
                  · 일반 뉴스·블로그·커뮤니티: 감점 없음 (0~-5)

                - GOV: 기본 100점. 법률·정책·통계 콘텐츠가 아닌 경우 거의 감점하지 마세요.
                  · 법령·정책 콘텐츠에서 공식 출처 없이 법 해석을 단정: -20~35
                  · 일반 뉴스·블로그·커뮤니티: 감점 없음 (0~-5)
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
