package com.shin.subscription.repository;

import com.shin.subscription.model.ChannelSubscription;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class ChannelSubscriptionRepository {

    private final DynamoDbTemplate dynamoDbTemplate;

    public ChannelSubscription findByPartitionKey(String partitionKey){
        QueryConditional keyCondition = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(partitionKey).build());

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(keyCondition)
                .build();

        return dynamoDbTemplate.query(queryRequest, ChannelSubscription.class)
                .items()
                .stream().findFirst().orElse(null);
    }

    public void save(ChannelSubscription channelSubscription){
        dynamoDbTemplate.save(channelSubscription);
    }

    public TransactWriteItem buildWriteTransactionItem(String channelId, String userId) {
       return TransactWriteItem.builder()
               .put(Put.builder()
                       .tableName("channel_subscriptions")
                       .item(Map.of(
                               "channelId", AttributeValue.builder().s(channelId).build(),
                               "userId", AttributeValue.builder().s(userId).build(),
                               "createdAt", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build()
                       ))
                       .conditionExpression("attribute_not_exists(channelId)")
                       .build())
               .build();
   }

   public TransactWriteItem buildDeleteTransactionWrite(String channelId, String userId) {
       return TransactWriteItem.builder()
               .delete(Delete.builder()
                       .tableName("channel_subscriptions")
                       .key(Map.of(
                               "channelId", AttributeValue.builder().s(channelId).build(),
                               "userId", AttributeValue.builder().s(userId).build()
                       ))
                       .conditionExpression("attribute_exists(channelId)")
                       .build())
               .build();
   }

}
