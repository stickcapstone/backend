package com.veriweb.veriweb_backend.controller;

import com.veriweb.veriweb_backend.common.response.ApiResponse;
import com.veriweb.veriweb_backend.dto.media.ImageAnalysisResponse;
import com.veriweb.veriweb_backend.dto.media.VideoAnalysisResponse;
import com.veriweb.veriweb_backend.service.media.MediaAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "미디어 AI 판별", description = "이미지 및 영상의 AI 생성 여부 분석")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MediaController {

    private final MediaAnalysisService mediaAnalysisService;

    @Operation(summary = "이미지 AI 판별", description = "이미지 파일을 업로드하면 AI 생성 여부를 분석합니다.")
    @PostMapping(value = "/image/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ImageAnalysisResponse> analyzeImage(
            @RequestPart("image") MultipartFile image) {
        return ApiResponse.ok(mediaAnalysisService.analyzeImage(image));
    }

    @Operation(summary = "영상 AI 판별", description = "영상 파일을 업로드하면 AI 생성 여부를 프레임별로 분석합니다.")
    @PostMapping(value = "/video/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<VideoAnalysisResponse> analyzeVideo(
            @RequestPart("video") MultipartFile video) {
        return ApiResponse.ok(mediaAnalysisService.analyzeVideo(video));
    }
}
