package com.veriweb.veriweb_backend.service.feed;

import com.veriweb.veriweb_backend.common.exception.ErrorCode;
import com.veriweb.veriweb_backend.common.exception.VeriWebException;
import com.veriweb.veriweb_backend.dto.feed.FeedArticleResponse;
import com.veriweb.veriweb_backend.dto.feed.FeedResponse;
import com.veriweb.veriweb_backend.entity.feed.ArticleCategory;
import com.veriweb.veriweb_backend.entity.feed.FeedArticle;
import com.veriweb.veriweb_backend.repository.feed.FeedArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final FeedArticleRepository feedArticleRepository;

    @Transactional(readOnly = true)
    public FeedResponse getFeed(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));

        Page<FeedArticle> result;
        if (category == null || category.isBlank()) {
            result = feedArticleRepository.findAll(pageable);
        } else {
            result = feedArticleRepository.findByCategory(parseCategory(category), pageable);
        }

        return FeedResponse.from(result);
    }

    @Transactional(readOnly = true)
    public FeedArticleResponse getArticle(Long articleId) {
        FeedArticle article = feedArticleRepository.findById(articleId)
                .orElseThrow(() -> new VeriWebException(ErrorCode.ARTICLE_NOT_FOUND));
        return FeedArticleResponse.from(article);
    }

    private ArticleCategory parseCategory(String category) {
        try {
            return ArticleCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new VeriWebException(ErrorCode.INVALID_CATEGORY);
        }
    }
}
