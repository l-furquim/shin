package com.shin.comment.producers;

public record CommentUpdatedEvent(
        String commentId,
        String videoId,
        String authorId
) {}
