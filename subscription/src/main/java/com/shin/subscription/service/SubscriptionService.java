package com.shin.subscription.service;

import com.shin.subscription.dto.CreateSubscriptionResponse;
import com.shin.subscription.dto.GetCreatorSubscriptionsResponse;
import com.shin.subscription.dto.RemoveSubscriptionResponse;

import java.util.UUID;

public interface SubscriptionService {

    CreateSubscriptionResponse subscribe(UUID userId, UUID channelId);

    RemoveSubscriptionResponse unsubscribe(UUID userId, UUID channelId);

    GetCreatorSubscriptionsResponse getSubscriptionInfo(UUID userId, UUID channelId);



}
