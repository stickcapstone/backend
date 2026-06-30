package com.veriweb.veriweb_backend.service.analysis;

import com.veriweb.veriweb_backend.common.exception.ErrorCode;
import com.veriweb.veriweb_backend.common.exception.VeriWebException;
import com.veriweb.veriweb_backend.dto.analysis.CrawledContent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CrawlerService {

    public CrawledContent crawl(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; VeriWeb/1.0; +https://veriweb.com)")
                    .timeout(15_000)
                    .get();

            String title = extractTitle(doc);
            String author = extractAuthor(doc);
            String bodyText = extractBodyText(doc);
            List<String> externalLinks = extractExternalLinks(doc, url);
            String publishedAt = extractPublishedAt(doc);
            String domain = new URL(url).getHost();
            String thumbnailUrl = extractThumbnailUrl(doc);

            return new CrawledContent(title, author, bodyText, externalLinks, publishedAt, domain, thumbnailUrl);
        } catch (VeriWebException e) {
            throw e;
        } catch (Exception e) {
            throw new VeriWebException(ErrorCode.CRAWL_FAILED);
        }
    }

    private String extractTitle(Document doc) {
        Element og = doc.selectFirst("meta[property=og:title]");
        if (og != null && !og.attr("content").isBlank()) {
            return og.attr("content");
        }
        return doc.title();
    }

    private String extractAuthor(Document doc) {
        Element authorMeta = doc.selectFirst("meta[name=author]");
        if (authorMeta != null && !authorMeta.attr("content").isBlank()) {
            return authorMeta.attr("content");
        }
        Element authorEl = doc.selectFirst("[rel=author], [itemprop=author], .author, .byline, .reporter");
        return authorEl != null && !authorEl.text().isBlank() ? authorEl.text() : "미확인";
    }

    private String extractBodyText(Document doc) {
        Element article = doc.selectFirst("article, main, [role=main], .article-body, .post-content, .entry-content, #article-body");
        Elements paragraphs = article != null ? article.select("p") : doc.select("p");

        String text = paragraphs.stream()
                .map(Element::text)
                .filter(t -> t.length() > 20)
                .collect(Collectors.joining("\n"));

        return text.length() > 4000 ? text.substring(0, 4000) : text;
    }

    private List<String> extractExternalLinks(Document doc, String pageUrl) {
        try {
            String pageHost = new URL(pageUrl).getHost();
            return doc.select("a[href]").stream()
                    .map(el -> el.attr("abs:href"))
                    .filter(href -> href.startsWith("http") && !href.contains(pageHost))
                    .distinct()
                    .limit(30)
                    .toList();
        } catch (MalformedURLException e) {
            return List.of();
        }
    }

    private String extractThumbnailUrl(Document doc) {
        Element og = doc.selectFirst("meta[property=og:image]");
        if (og != null && !og.attr("content").isBlank()) {
            return og.attr("content");
        }
        return null;
    }

    private String extractPublishedAt(Document doc) {
        Element articleTimeMeta = doc.selectFirst("meta[property=article:published_time]");
        if (articleTimeMeta != null) return articleTimeMeta.attr("content");

        Element dateTimeMeta = doc.selectFirst("meta[name=date]");
        if (dateTimeMeta != null) return dateTimeMeta.attr("content");

        Element timeEl = doc.selectFirst("time[datetime]");
        if (timeEl != null) return timeEl.attr("datetime");

        return null;
    }
}
