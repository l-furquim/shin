package com.shin.subscription.service;

import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

public interface SubscriptionCountService {

    TransactWriteItem buildDelta(String channelId, Long delta);
    long getCurrentCount(String channelId);

}
