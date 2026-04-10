package com.shin.search.service;

import com.shin.search.dto.SearchVideosResponse;
import com.shin.search.dto.VideoPublishedEvent;

import java.time.LocalDate;
import java.util.List;

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
            int maxResults,
            String pageToken
    );
}
