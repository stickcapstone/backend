package com.veriweb.veriweb_backend.controller;

import com.veriweb.veriweb_backend.common.response.ApiResponse;
import com.veriweb.veriweb_backend.dto.feed.FeedArticleResponse;
import com.veriweb.veriweb_backend.dto.feed.FeedResponse;
import com.veriweb.veriweb_backend.service.feed.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "신뢰도 피드", description = "카드형 뉴스 피드 목록 조회")
@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @Operation(summary = "피드 목록 조회", description = "카테고리 필터와 페이지네이션으로 신뢰도 피드 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<FeedResponse> getFeed(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(feedService.getFeed(category, page, size));
    }

    @Operation(summary = "피드 기사 단건 조회", description = "기사 ID로 피드 기사 상세 정보를 조회합니다.")
    @GetMapping("/{articleId}")
    public ApiResponse<FeedArticleResponse> getArticle(@PathVariable Long articleId) {
        return ApiResponse.ok(feedService.getArticle(articleId));
    }
}
