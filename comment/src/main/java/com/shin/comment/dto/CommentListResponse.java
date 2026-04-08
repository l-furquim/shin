package com.shin.comment.dto;

import com.shin.commons.models.PageInfo;

import java.util.List;

public record CommentListResponse(
        String nextPageToken,
        PageInfo pageInfo,
        List<CommentDto> items
) {}
