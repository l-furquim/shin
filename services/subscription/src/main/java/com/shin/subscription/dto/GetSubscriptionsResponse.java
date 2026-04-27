package com.shin.subscription.dto;

import com.shin.commons.models.PageInfo;

import java.util.List;

public record GetSubscriptionsResponse(
        PageInfo pageInfo,
        List<SubscriptionDto> items,
        String nextCursor
) {
}
