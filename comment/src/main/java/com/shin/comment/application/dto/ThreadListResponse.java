package com.shin.comment.application.dto;

import com.shin.comment.application.dto.ThreadDto;
import com.shin.commons.models.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ThreadListResponse", description = "Paginated list of threads")
public record ThreadListResponse(
        @Schema(description = "Opaque token used to retrieve the next page", nullable = true)
        String nextPageToken,
        @Schema(description = "Pagination metadata")
        PageInfo pageInfo,
        @Schema(description = "Page items")
        List<ThreadDto> items
) {}
