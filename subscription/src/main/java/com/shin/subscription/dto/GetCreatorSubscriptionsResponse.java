package com.shin.subscription.dto;

public record GetCreatorSubscriptionsResponse(
        boolean subscribed,
        Long subscribers
) {
}
