package com.shin.subscription.dto;


import java.util.UUID;

public record GetSubscriptionsRequest(
        UUID userId,
        UUID channelId,
        Boolean myRecentSubscribers,
        Boolean mine,
        Boolean mySubscribers,
        String cursor,
        int limit
) {
}
