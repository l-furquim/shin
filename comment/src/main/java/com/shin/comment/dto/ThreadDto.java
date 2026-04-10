package com.shin.comment.dto;

public record ThreadDto(
        String id,
        String videoId,
        String channelId,
        String authorId,
        String authorDisplayName,
        String authorAvatarUrl,
        String authorUrl,
        Long totalReplyCount,
        String createdAt,
        String updatedAt
) {}
