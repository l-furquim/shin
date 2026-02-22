package com.shin.user.dto;

public record RemoveSubscriptionResponse(
        boolean subscribed,
        Long subscribersCount
) {
}
