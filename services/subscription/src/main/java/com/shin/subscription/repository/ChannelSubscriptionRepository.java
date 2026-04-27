package com.shin.subscription.repository;

import com.shin.subscription.model.ChannelSubscription;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
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
@Component
public class ChannelSubscriptionRepository {

    private static final String TABLE_NAME = "channel_subscriptions";
    private static final String LSI_NAME = "channel_subscriptions_by_time";
    private static final Set<String> BASE_CURSOR_KEYS = Set.of("channelId", "userId");
    private static final Set<String> LSI_CURSOR_KEYS = Set.of("channelId", "userId", "createdAt");

    private final DynamoDbTemplate dynamoDbTemplate;
    private final DynamoDbEnhancedClient enhancedClient;

    public TransactWriteItem buildWriteTransactionItem(String channelId, String userId) {
        return TransactWriteItem.builder()
                .put(Put.builder()
                        .tableName(TABLE_NAME)
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
                        .tableName(TABLE_NAME)
                        .key(Map.of(
                                "channelId", AttributeValue.builder().s(channelId).build(),
                                "userId", AttributeValue.builder().s(userId).build()
                        ))
                        .conditionExpression("attribute_exists(channelId)")
                        .build())
                .build();
    }

    public QueryPage<ChannelSubscription> findByChannelId(String channelId, int limit, String cursor) {
        DynamoDbTable<ChannelSubscription> table = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(ChannelSubscription.class));

        QueryEnhancedRequest.Builder requestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue(channelId).build()))
                .limit(limit);

        if (cursor != null) {
            requestBuilder.exclusiveStartKey(CursorUtils.decode(cursor, BASE_CURSOR_KEYS));
        }

        Page<ChannelSubscription> page = table.query(requestBuilder.build()).iterator().next();
        String nextCursor = page.lastEvaluatedKey() != null ? CursorUtils.encode(page.lastEvaluatedKey()) : null;

        return new QueryPage<>(page.items(), nextCursor);
    }

    public QueryPage<ChannelSubscription> findRecentByChannelId(String channelId, int limit, String cursor) {
        DynamoDbTable<ChannelSubscription> table = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(ChannelSubscription.class));
        DynamoDbIndex<ChannelSubscription> index = table.index(LSI_NAME);

        QueryEnhancedRequest.Builder requestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue(channelId).build()))
                .scanIndexForward(false)
                .limit(limit);

        if (cursor != null) {
            requestBuilder.exclusiveStartKey(CursorUtils.decode(cursor, LSI_CURSOR_KEYS));
        }

        Page<ChannelSubscription> page = index.query(requestBuilder.build()).iterator().next();
        String nextCursor = page.lastEvaluatedKey() != null ? CursorUtils.encode(page.lastEvaluatedKey()) : null;

        return new QueryPage<>(page.items(), nextCursor);
    }
}
