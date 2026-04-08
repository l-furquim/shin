package com.shin.comment.producers;

public record CommentDeletedEvent(
        String commentId,
        String videoId,
        String authorId
) {}
