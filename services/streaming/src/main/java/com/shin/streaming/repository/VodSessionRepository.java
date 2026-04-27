package com.shin.streaming.repository;

import com.shin.streaming.model.PlaybackSession;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Repository
public class VodSessionRepository {

    private static final long SESSION_TTL_SECONDS = 24 * 60 * 60L;

    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbTemplate dynamoDbTemplate;

    @Value("${dynamo.vod-sessions-table}")
    private String tableName;

    public long accumulateWatchTime(String sessionId, String videoId, String userId, long watchTimeSeconds) {
        long expiresAt = Instant.now().plusSeconds(SESSION_TTL_SECONDS).getEpochSecond();

        UpdateItemResponse response = dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("sessionId", AttributeValue.fromS(sessionId)))
                .updateExpression(
                        "ADD accumulatedWatchSeconds :delta " +
                        "SET videoId = if_not_exists(videoId, :vid), " +
                            "userId = if_not_exists(userId, :uid), " +
                            "expiresAt = :exp"
                )
                .expressionAttributeValues(Map.of(
                        ":delta", AttributeValue.fromN(String.valueOf(watchTimeSeconds)),
                        ":vid",   AttributeValue.fromS(videoId),
                        ":uid",   AttributeValue.fromS(userId),
                        ":exp",   AttributeValue.fromN(String.valueOf(expiresAt))
                ))
                .returnValues(ReturnValue.ALL_NEW)
                .build());

        AttributeValue accumulated = response.attributes().get("accumulatedWatchSeconds");
        return accumulated != null ? Long.parseLong(accumulated.n()) : watchTimeSeconds;
    }

    public Optional<PlaybackSession> findById(String sessionId) {
        QueryConditional keyCondition = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(sessionId).build());

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(keyCondition)
                .build();

        return dynamoDbTemplate.query(queryRequest, PlaybackSession.class)
                .items()
                .stream()
                .findFirst();
    }

    public boolean markViewCounted(String sessionId) {
        try {
            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("sessionId", AttributeValue.fromS(sessionId)))
                    .updateExpression("SET viewCounted = :true")
                    .conditionExpression("attribute_not_exists(viewCounted) OR viewCounted = :false")
                    .expressionAttributeValues(Map.of(
                            ":true",  AttributeValue.fromBool(true),
                            ":false", AttributeValue.fromBool(false)
                    ))
                    .build());
            return true;
        } catch (ConditionalCheckFailedException e) {
            log.debug("View already counted for sessionId={}", sessionId);
            return false;
        }
    }
}
