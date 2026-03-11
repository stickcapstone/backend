package com.veriweb.veriweb_backend.dto.feed;

import com.veriweb.veriweb_backend.entity.feed.FeedArticle;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class FeedResponse {

    private final List<ArticleItem> articles;
    private final long totalCount;
    private final int page;
    private final int size;
    private final boolean hasNext;

    public static FeedResponse from(Page<FeedArticle> pageResult) {
        return new FeedResponse(pageResult);
    }

    private FeedResponse(Page<FeedArticle> p) {
        this.articles = p.getContent().stream().map(ArticleItem::from).toList();
        this.totalCount = p.getTotalElements();
        this.page = p.getNumber();
        this.size = p.getSize();
        this.hasNext = p.hasNext();
    }

    @Getter
    public static class ArticleItem {
        private final Long id;
        private final String title;
        private final String url;
        private final String source;
        private final String thumbnailUrl;
        private final String category;
        private final int trustScore;
        private final String grade;
        private final LocalDateTime publishedAt;

        public static ArticleItem from(FeedArticle article) {
            return new ArticleItem(article);
        }

        private ArticleItem(FeedArticle a) {
            this.id = a.getId();
            this.title = a.getTitle();
            this.url = a.getUrl();
            this.source = a.getSource();
            this.thumbnailUrl = a.getThumbnailUrl();
            this.category = a.getCategory().name();
            this.trustScore = a.getTrustScore();
            this.grade = a.getGrade().name();
            this.publishedAt = a.getPublishedAt();
        }
    }
}
