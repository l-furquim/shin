package com.shin.subscription.service;

import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

public interface UserSubscriptionService {
    TransactWriteItem buildTransaction(String userId, String channelId);
    TransactWriteItem buildDeleteTransaction(String userId, String channelId);
    boolean isUserSubscribed(String userId, String channelId);
}
