package com.shin.comment.infrastructure.dto;

public record CommentUpdatedEvent(
        String commentId,
        String videoId,
        String authorId
) {}
