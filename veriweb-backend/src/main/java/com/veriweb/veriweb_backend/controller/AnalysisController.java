package com.veriweb.veriweb_backend.controller;

import com.veriweb.veriweb_backend.common.response.ApiResponse;
import com.veriweb.veriweb_backend.dto.analysis.AnalysisResponse;
import com.veriweb.veriweb_backend.dto.analysis.AnalyzeRequest;
import com.veriweb.veriweb_backend.service.analysis.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "신뢰도 분석", description = "URL 신뢰도 분석 요청 및 결과 조회")
@RestController
@RequestMapping("/api/v1/analyze")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(summary = "URL 분석 요청", description = "URL을 입력받아 신뢰도 분석을 수행하고 결과를 반환합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AnalysisResponse> analyze(@Valid @RequestBody AnalyzeRequest request) {
        return ApiResponse.ok(analysisService.analyze(request.getUrl()));
    }

    @Operation(summary = "분석 결과 조회", description = "분석 ID로 저장된 신뢰도 분석 결과를 조회합니다.")
    @GetMapping("/{analysisId}")
    public ApiResponse<AnalysisResponse> getAnalysis(@PathVariable Long analysisId) {
        return ApiResponse.ok(analysisService.getById(analysisId));
    }
}
