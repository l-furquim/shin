package com.shin.subscription.dto;

public record RemoveSubscriptionResponse(
        boolean subscribed,
        Long subscribersCount
) {
}
