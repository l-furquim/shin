package com.shin.search.service;

import com.shin.search.dto.SearchVideosResponse;
import com.shin.search.dto.VideoPublishedEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SearchService {

    void indexVideo(VideoPublishedEvent event);

    SearchVideosResponse search(
            String query,
            List<String> tags,
            String language,
            String category,
            LocalDate dateFrom,
            LocalDate dateTo,
            Boolean forAdults,
            boolean forMine,
            int maxResults,
            String pageToken,
            UUID userId
    );
}
