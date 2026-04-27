package com.shin.comment.infrastructure.dto;

public record CommentReplyCreatedEvent(
        String commentId,
        String parentId,
        String videoId,
        String authorId
) {}
