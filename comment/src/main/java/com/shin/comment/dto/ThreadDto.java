package com.shin.comment.dto;

public record ThreadDto(
        String id,
        String videoId,
        String channelId,
        String authorId,
        Long totalReplyCount,
        String createdAt,
        String updatedAt
) {}
