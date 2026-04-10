package com.shin.search.controller;

import com.shin.search.dto.SearchVideosResponse;
import com.shin.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("${api.version}/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<SearchVideosResponse> searchVideos(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "false") boolean forAdults,
            @RequestParam(defaultValue = "20") int maxResults,
            @RequestParam(required = false) String pageToken
    ) {
        maxResults = Math.min(Math.max(maxResults, 1), 50);

        List<String> tagList = (tags != null && !tags.isBlank())
                ? Arrays.stream(tags.split(",")).map(String::trim).filter(t -> !t.isEmpty()).toList()
                : null;

        SearchVideosResponse response = searchService.search(
                q, tagList, language, category, dateFrom, dateTo,
                forAdults ? null : false,
                maxResults, pageToken
        );

        return ResponseEntity.ok(response);
    }
}
