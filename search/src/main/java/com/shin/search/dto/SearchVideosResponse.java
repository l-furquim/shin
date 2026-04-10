package com.shin.search.dto;

import com.shin.commons.models.PageInfo;

import java.util.List;

public record SearchVideosResponse(
        String nextPageToken,
        PageInfo pageInfo,
        List<VideoDto> results
) {
}
