package com.shin.metadata.dto;

public record ThreadCreatedEvent(
        String threadId,
        String videoId,
        String channelId,
        String authorId
) {}
