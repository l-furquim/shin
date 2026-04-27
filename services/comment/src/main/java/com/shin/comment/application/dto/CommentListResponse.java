package com.shin.comment.application.dto;

import com.shin.comment.application.dto.CommentDto;
import com.shin.commons.models.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "CommentListResponse", description = "Paginated list of comments")
public record CommentListResponse(
        @Schema(description = "Opaque token used to retrieve the next page", nullable = true)
        String nextPageToken,
        @Schema(description = "Pagination metadata")
        PageInfo pageInfo,
        @Schema(description = "Page items")
        List<CommentDto> items
) {}
