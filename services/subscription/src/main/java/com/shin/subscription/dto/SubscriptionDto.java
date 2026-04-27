package com.shin.subscription.dto;

public record SubscriptionDto(
        String channelId,
        String userId,
        Long subscribedAt
) {
}
