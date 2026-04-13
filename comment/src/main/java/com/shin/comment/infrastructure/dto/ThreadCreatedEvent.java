package com.shin.comment.infrastructure.dto;

public record ThreadCreatedEvent(
        String threadId,
        String videoId,
        String channelId,
        String authorId
) {}
