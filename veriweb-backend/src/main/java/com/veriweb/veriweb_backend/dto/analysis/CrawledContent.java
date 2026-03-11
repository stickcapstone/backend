package com.veriweb.veriweb_backend.dto.analysis;

import java.util.List;

public record CrawledContent(
        String title,
        String author,
        String bodyText,
        List<String> externalLinks,
        String publishedAt,
        String domain
) {}
