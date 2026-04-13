package com.shin.comment.infrastructure.gateway;

import com.shin.comment.domain.gateway.ThreadGateway;
import com.shin.comment.domain.model.Thread;
import com.shin.comment.infrastructure.entity.ThreadEntity;
import com.shin.comment.infrastructure.dto.ThreadQueryPage;
import com.shin.comment.infrastructure.dto.ThreadScanPage;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class ThreadGatewayImpl implements ThreadGateway {

    private static final String TABLE_NAME = "comment_threads";

    private final DynamoDbTemplate dynamoDbTemplate;
    private final DynamoDbClient dynamoDbClient;

    @Override
    public Thread save(Thread thread) {
        ThreadEntity entity = ThreadEntity.builder()
                .topLevelCommentId(thread.getTopLevelCommentId())
                .videoId(thread.getVideoId())
                .channelId(thread.getChannelId())
                .authorId(thread.getAuthorId())
                .authorDisplayName(thread.getAuthorDisplayName())
                .authorAvatarUrl(thread.getAuthorAvatarUrl())
                .authorLink(thread.getAuthorLink())
                .totalReplyCount(thread.getTotalReplyCount())
                .createdAt(thread.getCreatedAt())
                .updatedAt(thread.getUpdatedAt())
                .build();
        dynamoDbTemplate.save(entity);
        return thread;
    }

    @Override
    public ThreadQueryPage findByVideoId(
            String videoId, int limit, Map<String, AttributeValue> exclusiveStartKey) {

        QueryRequest.Builder req = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .keyConditionExpression("videoId = :vid")
                .expressionAttributeValues(Map.of(
                        ":vid", AttributeValue.fromS(videoId)
                ))
                .limit(limit);

        if (exclusiveStartKey != null && !exclusiveStartKey.isEmpty()) {
            req.exclusiveStartKey(exclusiveStartKey);
        }

        QueryResponse response = dynamoDbClient.query(req.build());

        return new ThreadQueryPage(
                response.items().stream().map(this::toThread).toList(),
                response.lastEvaluatedKey()
        );
    }

    @Override
    public ThreadScanPage findByIds(
            List<String> ids, int limit, Map<String, AttributeValue> exclusiveStartKey) {

        List<Thread> results = ids.stream()
                .map(id -> dynamoDbClient.query(QueryRequest.builder()
                        .tableName(TABLE_NAME)
                        .indexName("topLevelCommentIdIndex")
                        .keyConditionExpression("topLevelCommentId = :id")
                        .expressionAttributeValues(Map.of(":id", AttributeValue.fromS(id)))
                        .build()))
                .flatMap(response -> response.items().stream())
                .map(this::toThread)
                .toList();

        return new ThreadScanPage(results, Map.of());
    }

    @Override
    public ThreadScanPage findByChannelId(
            String channelId, int limit, Map<String, AttributeValue> exclusiveStartKey) {

        QueryRequest.Builder req = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .indexName("channelIdIndex")
                .keyConditionExpression("channelId = :cid")
                .expressionAttributeValues(Map.of(
                        ":cid", AttributeValue.fromS(channelId)
                ))
                .limit(limit);

        if (exclusiveStartKey != null && !exclusiveStartKey.isEmpty()) {
            req.exclusiveStartKey(exclusiveStartKey);
        }

        QueryResponse response = dynamoDbClient.query(req.build());

        return new ThreadScanPage(
                response.items().stream().map(this::toThread).toList(),
                response.lastEvaluatedKey()
        );
    }

    private Thread toThread(Map<String, AttributeValue> item) {
        return Thread.builder()
                .videoId(attrS(item, "videoId"))
                .topLevelCommentId(attrS(item, "topLevelCommentId"))
                .channelId(attrS(item, "channelId"))
                .authorId(attrS(item, "authorId"))
                .authorDisplayName(attrS(item, "authorDisplayName"))
                .authorAvatarUrl(attrS(item, "authorAvatarUrl"))
                .authorLink(attrS(item, "authorLink"))
                .totalReplyCount(item.containsKey("totalReplyCount")
                        ? Long.parseLong(item.get("totalReplyCount").n()) : 0L)
                .createdAt(attrS(item, "createdAt"))
                .updatedAt(attrS(item, "updatedAt"))
                .build();
    }

    private String attrS(Map<String, AttributeValue> item, String key) {
        return item.containsKey(key) ? item.get(key).s() : null;
    }
}
