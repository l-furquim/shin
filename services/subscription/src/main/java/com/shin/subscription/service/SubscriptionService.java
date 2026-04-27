package com.shin.subscription.service;

import com.shin.subscription.dto.*;

import java.util.UUID;

public interface SubscriptionService {

    CreateSubscriptionResponse subscribe(UUID userId, UUID channelId);

    RemoveSubscriptionResponse unsubscribe(UUID userId, UUID channelId);

    GetCreatorSubscriptionsResponse getSubscriptionInfo(UUID userId, UUID channelId);

    GetSubscriptionsResponse getSubscriptions(
           GetSubscriptionsRequest request
    );

}
