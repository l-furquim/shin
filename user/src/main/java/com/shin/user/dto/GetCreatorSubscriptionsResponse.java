package com.shin.user.dto;

public record GetCreatorSubscriptionsResponse(
        boolean subscribed,
        Long subscribers
) {
}
