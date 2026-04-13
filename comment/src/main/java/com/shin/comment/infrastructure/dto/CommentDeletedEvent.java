package com.shin.comment.infrastructure.dto;

public record CommentDeletedEvent(
        String commentId,
        String videoId,
        String authorId
) {}
