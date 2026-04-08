package com.shin.comment.producers;

public record CommentReplyCreatedEvent(
        String commentId,
        String parentId,
        String videoId,
        String authorId
) {}
