package com.shin.user.dto;

public record CreateSubscriptionResponse(
        boolean subscribed,
        Long subscribersCount
) {
}
