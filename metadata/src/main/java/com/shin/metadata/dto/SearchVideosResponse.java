package com.shin.metadata.dto;

import com.shin.commons.models.PageInfo;

import java.util.List;

public record SearchVideosResponse(
        String nextPageToken,
        String prevPageToken,
        PageInfo pageInfo,
        List<VideoDto> items
) {}
