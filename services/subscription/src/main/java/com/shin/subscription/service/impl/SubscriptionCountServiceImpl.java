package com.shin.subscription.service.impl;

import com.shin.subscription.model.SubscriptionCount;
import com.shin.subscription.service.SubscriptionCountService;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.Update;

import java.util.Map;

@RequiredArgsConstructor
@Service
public class SubscriptionCountServiceImpl implements SubscriptionCountService {

    private final DynamoDbTemplate dynamoDbTemplate;

    @Override
    public TransactWriteItem buildDelta(String channelId, Long delta) {
        return TransactWriteItem.builder()
                .update(Update.builder()
                        .tableName("channel_subscription_counters")
                        .key(Map.of(
                                "channelId", AttributeValue.builder().s(channelId).build()
                        ))
                        .updateExpression("ADD subscribersCount :delta")
                        .expressionAttributeValues(Map.of(
                                ":delta", AttributeValue.builder().n(String.valueOf(delta)).build()
                        ))
                        .build())
                .build();
    }

    @Override
    public long getCurrentCount(String channelId) {
        QueryConditional keyCondition = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(channelId).build());

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(keyCondition)
                .build();

        return dynamoDbTemplate.query(queryRequest, SubscriptionCount.class)
                .items()
                .stream().findFirst().map(SubscriptionCount::getSubscribersCount).orElse(0L);
    }
}
