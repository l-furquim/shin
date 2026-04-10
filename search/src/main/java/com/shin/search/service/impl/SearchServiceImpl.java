package com.shin.search.service.impl;

import com.shin.commons.models.PageInfo;
import com.shin.commons.util.PageTokenUtil;
import com.shin.search.dto.SearchVideosResponse;
import com.shin.search.dto.VideoDto;
import com.shin.search.dto.VideoPublishedEvent;
import com.shin.search.service.SearchService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.*;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private static final String INDEX = "videos";

    private final OpenSearchClient client;

    @PostConstruct
    void initIndex() {
        try {
            boolean exists = client.indices().exists(ExistsRequest.of(e -> e.index(INDEX))).value();
            if (!exists) {
                client.indices().create(CreateIndexRequest.of(c -> c
                        .index(INDEX)
                        .mappings(m -> m
                                .properties("id", p -> p.keyword(k -> k))
                                .properties("title", p -> p.text(t -> t.analyzer("standard")))
                                .properties("description", p -> p.text(t -> t.analyzer("standard")))
                                .properties("categoryName", p -> p.keyword(k -> k))
                                .properties("channelName", p -> p.text(t -> t
                                        .fields("keyword", f -> f.keyword(k -> k))))
                                .properties("channelAvatar", p -> p.keyword(k -> k.index(false)))
                                .properties("language", p -> p.keyword(k -> k))
                                .properties("tags", p -> p.keyword(k -> k))
                                .properties("publishedAt", p -> p.date(d -> d
                                        .format("strict_date_time||strict_date_time_no_millis||date_optional_time")))
                                .properties("duration", p -> p.double_(d -> d))
                                .properties("thumbnailUrl", p -> p.keyword(k -> k.index(false)))
                                .properties("videoLink", p -> p.keyword(k -> k))
                                .properties("forAdults", p -> p.boolean_(b -> b))
                        )
                ));
                log.info("Created OpenSearch index '{}'", INDEX);
            }
        } catch (IOException e) {
            log.error("Failed to initialize OpenSearch index '{}': {}", INDEX, e.getMessage(), e);
        }
    }

    @Override
    public void indexVideo(VideoPublishedEvent event) {
        try {
            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("id", event.id());
            doc.put("title", event.title());
            doc.put("description", event.description());
            doc.put("categoryName", event.categoryName());
            doc.put("channelName", event.channelName());
            doc.put("channelAvatar", event.channelAvatar());
            doc.put("language", event.language());
            doc.put("tags", event.tags() != null ? event.tags() : List.of());
            doc.put("publishedAt", event.publishedAt());
            doc.put("duration", event.duration());
            doc.put("thumbnailUrl", event.thumbnailUrl());
            doc.put("videoLink", event.videoLink());
            doc.put("forAdults", event.forAdults());

            client.index(IndexRequest.of(i -> i
                    .index(INDEX)
                    .id(event.id())
                    .document(doc)
            ));
            log.info("Indexed video id={}", event.id());
        } catch (IOException e) {
            log.error("Failed to index video id={}: {}", event.id(), e.getMessage(), e);
            throw new RuntimeException("Failed to index video", e);
        }
    }

    @Override
    public SearchVideosResponse search(
            String query,
            List<String> tags,
            String language,
            String category,
            LocalDate dateFrom,
            LocalDate dateTo,
            Boolean forAdults,
            int maxResults,
            String pageToken
    ) {
        try {
            List<String> searchAfter = decodePageToken(pageToken);

            BoolQuery.Builder bool = new BoolQuery.Builder();

            // Full-text on title, description, tags
            if (query != null && !query.isBlank()) {
                bool.must(Query.of(q -> q.multiMatch(mm -> mm
                        .query(query)
                        .fields("title^3", "description", "tags^2")
                        .fuzziness("AUTO")
                )));
            } else {
                bool.must(Query.of(q -> q.matchAll(ma -> ma)));
            }

            // Filters
            if (language != null && !language.isBlank()) {
                bool.filter(Query.of(q -> q.term(t -> t.field("language").value(FieldValue.of(language)))));
            }
            if (category != null && !category.isBlank()) {
                bool.filter(Query.of(q -> q.term(t -> t.field("categoryName").value(FieldValue.of(category)))));
            }
            if (tags != null && !tags.isEmpty()) {
                List<FieldValue> tagValues = tags.stream().map(FieldValue::of).toList();
                bool.filter(Query.of(q -> q.terms(t -> t
                        .field("tags")
                        .terms(tv -> tv.value(tagValues))
                )));
            }
            if (Boolean.FALSE.equals(forAdults)) {
                bool.filter(Query.of(q -> q.term(t -> t.field("forAdults").value(FieldValue.FALSE))));
            }
            if (dateFrom != null || dateTo != null) {
                bool.filter(Query.of(q -> q.range(r -> {
                    var range = r.field("publishedAt");
                    if (dateFrom != null)
                        range = range.gte(org.opensearch.client.json.JsonData.of(dateFrom.toString()));
                    if (dateTo != null) range = range.lte(org.opensearch.client.json.JsonData.of(dateTo.toString()));
                    return range;
                })));
            }

            int fetchSize = maxResults + 1;

            SearchRequest.Builder reqBuilder = new SearchRequest.Builder()
                    .index(INDEX)
                    .size(fetchSize)
                    .query(Query.of(q -> q.bool(bool.build())))
                    .sort(s -> s.field(f -> f.field("publishedAt").order(SortOrder.Desc)))
                    .sort(s -> s.field(f -> f.field("id").order(SortOrder.Asc)));

            if (!searchAfter.isEmpty()) {
                reqBuilder.searchAfter(searchAfter);
            }

            SearchResponse<Map> response = client.search(reqBuilder.build(), Map.class);

            List<Hit<Map>> hits = response.hits().hits();
            boolean hasMore = hits.size() > maxResults;
            if (hasMore) hits = hits.subList(0, maxResults);

            List<VideoDto> items = hits.stream().map(this::toDto).toList();

            String nextPageToken = null;
            if (hasMore && !items.isEmpty()) {
                VideoDto last = items.getLast();
                nextPageToken = PageTokenUtil.encode("pt", last.publishedAt(), "id", last.id());
            }

            long totalHits = response.hits().total() != null ? response.hits().total().value() : items.size();

            return new SearchVideosResponse(nextPageToken, new PageInfo(totalHits, (long) maxResults), items);

        } catch (IOException e) {
            log.error("Search failed: {}", e.getMessage(), e);
            throw new RuntimeException("Search failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private VideoDto toDto(Hit<Map> hit) {
        Map<String, Object> src = hit.source() != null ? hit.source() : Map.of();
        return new VideoDto(
                str(src, "id"),
                str(src, "title"),
                str(src, "description"),
                str(src, "categoryName"),
                str(src, "channelName"),
                str(src, "channelAvatar"),
                src.get("duration") instanceof Number n ? n.doubleValue() : null,
                str(src, "thumbnailUrl"),
                str(src, "videoLink"),
                str(src, "language"),
                Boolean.TRUE.equals(src.get("forAdults")),
                src.get("tags") instanceof List<?> l ? l.stream().map(Object::toString).toList() : List.of(),
                hit.score(),
                str(src, "publishedAt")
        );
    }

    private String str(Map<String, Object> src, String key) {
        Object v = src.get(key);
        return v != null ? v.toString() : null;
    }

    private List<String> decodePageToken(String pageToken) {
        if (pageToken == null || pageToken.isBlank()) return List.of();

        try {
            Map<String, String> parts = PageTokenUtil.decode(pageToken);

            String pt = parts.get("pt");
            String id = parts.get("id");

            if (pt == null || id == null) return List.of();

            return List.of(pt, id);

        } catch (Exception e) {
            log.warn("Invalid pageToken, ignoring: {}", e.getMessage());
            return List.of();
        }
    }
}
