package com.shin.user.service;

import com.shin.user.dto.CreateSubscriptionResponse;
import com.shin.user.dto.GetCreatorSubscriptionsResponse;
import com.shin.user.dto.RemoveSubscriptionResponse;

import java.util.UUID;

public interface SubscriptionService {

    CreateSubscriptionResponse subscribe(UUID userId, UUID channelId);

    RemoveSubscriptionResponse unsubscribe(UUID userId, UUID channelId);

    GetCreatorSubscriptionsResponse getSubscriptionInfo(UUID userId, UUID channelId);

}
