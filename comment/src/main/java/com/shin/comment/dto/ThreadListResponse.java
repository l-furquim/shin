package com.shin.comment.dto;

import com.shin.commons.models.PageInfo;

import java.util.List;

public record ThreadListResponse(
        String nextPageToken,
        PageInfo pageInfo,
        List<ThreadDto> items
) {}
