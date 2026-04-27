package com.shin.subscription.service.impl;

import com.shin.subscription.model.UserSubscriptions;
import com.shin.subscription.repository.CursorUtils;
import com.shin.subscription.repository.QueryPage;
import com.shin.subscription.service.UserSubscriptionService;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class UserSubscriptionServiceImpl implements UserSubscriptionService {

    private static final String TABLE_NAME = "user_subscriptions";
    private static final Set<String> CURSOR_KEYS = Set.of("userId", "channelId");

    private final DynamoDbTemplate dynamoDbTemplate;
    private final DynamoDbEnhancedClient enhancedClient;

    @Override
    public TransactWriteItem buildTransaction(String userId, String channelId) {
        return TransactWriteItem.builder()
                .put(Put.builder()
                        .tableName(TABLE_NAME)
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
                        .tableName(TABLE_NAME)
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

    @Override
    public QueryPage<UserSubscriptions> findByUserId(String userId, int limit, String cursor) {
        DynamoDbTable<UserSubscriptions> table = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(UserSubscriptions.class));

        QueryEnhancedRequest.Builder requestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue(userId).build()))
                .limit(limit);

        if (cursor != null) {
            requestBuilder.exclusiveStartKey(CursorUtils.decode(cursor, CURSOR_KEYS));
        }

        Page<UserSubscriptions> page = table.query(requestBuilder.build()).iterator().next();
        String nextCursor = page.lastEvaluatedKey() != null ? CursorUtils.encode(page.lastEvaluatedKey()) : null;

        return new QueryPage<>(page.items(), nextCursor);
    }
}
