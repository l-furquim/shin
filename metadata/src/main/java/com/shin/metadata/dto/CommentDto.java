package com.shin.metadata.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentDto(
        UUID id,
        AuthorDetails authorInformation,
        UUID channelId,
        String content,
        UUID parentId,
        boolean canRate,
        String viewRating,
        Long likeCount,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {
}
