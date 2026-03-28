package com.shin.metadata.dto;

import com.shin.commons.models.PageInfo;

import java.util.List;


public record SearchCommentResponse(
        String nextPageToken,
        PageInfo pageInfo,
        List<CommentDto> items
) {
}
