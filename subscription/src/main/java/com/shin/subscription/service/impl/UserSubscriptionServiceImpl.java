package com.shin.subscription.service.impl;

import com.shin.subscription.model.UserSubscriptions;
import com.shin.subscription.service.UserSubscriptionService;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

import java.util.Map;

@RequiredArgsConstructor
@Service
public class UserSubscriptionServiceImpl implements UserSubscriptionService {

    private final DynamoDbTemplate dynamoDbTemplate;

    @Override
    public TransactWriteItem buildTransaction(String userId, String channelId) {
        return TransactWriteItem.builder()
                .put(Put.builder()
                        .tableName("user_subscriptions")
                        .item(Map.of(
                                "userId", AttributeValue.builder().s(userId).build(),
                                "channelId", AttributeValue.builder().s(channelId).build()
                        ))
                        .conditionExpression("attribute_not_exists(channelId)")
                        .build())
                .build();
    }

    @Override
    public TransactWriteItem buildDeleteTransaction(String userId, String channelId) {
        return TransactWriteItem.builder()
                .delete(Delete.builder()
                        .tableName("user_subscriptions")
                        .key(Map.of(
                                "userId", AttributeValue.builder().s(userId).build(),
                                "channelId", AttributeValue.builder().s(channelId).build()
                        ))
                        .conditionExpression("attribute_exists(userId)")
                        .build())
                .build();
    }

    @Override
    public boolean isUserSubscribed(String userId, String channelId) {
        QueryConditional keyCondition = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(userId)
                .sortValue(channelId)
                .build());

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(keyCondition)
                .build();

        return dynamoDbTemplate.query(queryRequest, UserSubscriptions.class)
                .items()
                .stream().findFirst().isPresent();
    }
}
