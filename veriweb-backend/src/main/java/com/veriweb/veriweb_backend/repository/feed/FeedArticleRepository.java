package com.veriweb.veriweb_backend.repository.feed;

import com.veriweb.veriweb_backend.entity.feed.ArticleCategory;
import com.veriweb.veriweb_backend.entity.feed.FeedArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedArticleRepository extends JpaRepository<FeedArticle, Long> {
    Page<FeedArticle> findByCategory(ArticleCategory category, Pageable pageable);
}
