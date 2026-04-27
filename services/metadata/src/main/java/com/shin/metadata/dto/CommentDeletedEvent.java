package com.shin.metadata.dto;

public record CommentDeletedEvent(
        String commentId,
        String videoId,
        String authorId
) {}
