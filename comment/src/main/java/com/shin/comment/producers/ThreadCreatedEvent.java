package com.shin.comment.producers;

public record ThreadCreatedEvent(
        String threadId,
        String videoId,
        String channelId,
        String authorId
) {}
