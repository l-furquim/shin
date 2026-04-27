package com.shin.subscription.service;

import com.shin.subscription.model.UserSubscriptions;
import com.shin.subscription.repository.QueryPage;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

public interface UserSubscriptionService {
    TransactWriteItem buildTransaction(String userId, String channelId);
    TransactWriteItem buildDeleteTransaction(String userId, String channelId);
    boolean isUserSubscribed(String userId, String channelId);
    QueryPage<UserSubscriptions> findByUserId(String userId, int limit, String cursor);
}
