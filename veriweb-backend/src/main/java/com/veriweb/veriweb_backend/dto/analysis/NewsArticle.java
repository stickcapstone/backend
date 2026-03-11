package com.veriweb.veriweb_backend.dto.analysis;

public record NewsArticle(
        String title,
        String url,
        String source,
        String publishedAt
) {}
