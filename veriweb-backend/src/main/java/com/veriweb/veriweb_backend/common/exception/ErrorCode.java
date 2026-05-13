package com.veriweb.veriweb_backend.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Analysis
    MISSING_URL(HttpStatus.BAD_REQUEST, "URL을 입력해주세요."),
    INVALID_URL(HttpStatus.BAD_REQUEST, "올바른 URL 형식이 아닙니다."),
    PAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "페이지를 찾을 수 없습니다."),
    ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "분석 결과를 찾을 수 없습니다."),
    CRAWL_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "해당 페이지에 접근할 수 없습니다."),

    // Feed
    ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "기사를 찾을 수 없습니다."),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "유효하지 않은 카테고리입니다."),

    // Media
    MEDIA_FILE_REQUIRED(HttpStatus.BAD_REQUEST, "파일을 첨부해주세요."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),
    MEDIA_ANALYSIS_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "미디어 분석에 실패했습니다."),
    HIVE_API_KEY_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "Hive API 키가 설정되지 않았습니다."),

    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
