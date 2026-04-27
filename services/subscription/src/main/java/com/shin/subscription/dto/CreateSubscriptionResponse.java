package com.shin.subscription.dto;

public record CreateSubscriptionResponse(
        boolean subscribed,
        Long subscribersCount
) {
}
