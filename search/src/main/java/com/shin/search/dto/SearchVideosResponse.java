package com.shin.search.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.shin.commons.models.PageInfo;

import java.util.List;

public record SearchVideosResponse(
        String nextPageToken,
        String prevPageToken,
        PageInfo pageInfo,
        @JsonAlias("items")
        List<VideoDto> results
) {
}
