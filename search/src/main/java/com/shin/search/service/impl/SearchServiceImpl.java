package com.shin.search.service.impl;

import com.shin.commons.models.PageInfo;
import com.shin.search.client.MetadataClient;
import com.shin.search.dto.SearchVideosResponse;
import com.shin.search.dto.VideoPublishedEvent;
import com.shin.search.exception.handler.MetadataFetchException;
import com.shin.search.service.SearchService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private static final String INDEX = "videos";
    private static final String METADATA_FIELDS = "thumbnails,statistics,channel,tags,contentDetails";

    private final OpenSearchClient client;
    private final MetadataClient metadataClient;

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
            String pageToken,
            UUID userId
    ) {
        try {
            List<String> ids = searchVideoIds(query, tags, language, category, dateFrom, dateTo, forAdults);
            if (ids.isEmpty()) {
                return new SearchVideosResponse(null, null, new PageInfo(0L, (long) maxResults), List.of());
            }

            return fetchVideos(ids, pageToken, maxResults, userId);
        } catch (IOException e) {
            log.error("Search failed: {}", e.getMessage(), e);
            throw new RuntimeException("Search failed", e);
        }
    }

    private List<String> searchVideoIds(
            String query,
            List<String> tags,
            String language,
            String category,
            LocalDate dateFrom,
            LocalDate dateTo,
            Boolean forAdults
    ) throws IOException {
        BoolQuery.Builder bool = new BoolQuery.Builder();

        if (query != null && !query.isBlank()) {
            bool.must(Query.of(q -> q.multiMatch(mm -> mm
                    .query(query)
                    .fields("title^3", "description", "tags^2")
                    .fuzziness("AUTO")
            )));
        } else {
            bool.must(Query.of(q -> q.matchAll(ma -> ma)));
        }

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
                if (dateFrom != null) {
                    range = range.gte(JsonData.of(LocalDateTime.of(dateFrom, java.time.LocalTime.MIN).toString()));
                }
                if (dateTo != null) {
                    range = range.lte(JsonData.of(LocalDateTime.of(dateTo, java.time.LocalTime.MAX).toString()));
                }
                return range;
            })));
        }

        SearchRequest request = new SearchRequest.Builder()
                .index(INDEX)
                .size(1000)
                .query(Query.of(q -> q.bool(bool.build())))
                .sort(s -> s.field(f -> f.field("publishedAt").order(SortOrder.Desc)))
                .sort(s -> s.field(f -> f.field("id").order(SortOrder.Asc)))
                .source(s -> s.filter(f -> f.includes("id")))
                .build();

        SearchResponse<Map> response = client.search(request, Map.class);
        List<Hit<Map>> hits = response.hits().hits();

        List<String> ids = new ArrayList<>(hits.size());
        for (Hit<Map> hit : hits) {
            if (hit.id() != null && !hit.id().isBlank()) {
                ids.add(hit.id());
                continue;
            }
            Map source = hit.source();
            if (source != null && source.get("id") != null) {
                ids.add(source.get("id").toString());
            }
        }
        return ids;
    }

    private SearchVideosResponse fetchVideos(
            List<String> ids,
            String cursor,
            int limit,
            UUID userId
    ) {
        try {
            ResponseEntity<SearchVideosResponse> response = metadataClient.search(
                    null,
                    String.join(",", ids),
                    null,
                    METADATA_FIELDS,
                    null,
                    null,
                    cursor,
                    limit,
                    userId
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("Could not fetch videos from metadata status={} body={}", response.getStatusCode(), response.getBody());
                throw new MetadataFetchException();
            }

            SearchVideosResponse body = response.getBody();
            return new SearchVideosResponse(
                    body.nextPageToken(),
                    body.prevPageToken(),
                    body.pageInfo(),
                    body.results()
            );
        } catch (MetadataFetchException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching videos from metadata", e);
            throw new MetadataFetchException();
        }
    }
}
