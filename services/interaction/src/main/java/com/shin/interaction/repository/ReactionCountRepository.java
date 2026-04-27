package com.shin.interaction.repository;

import com.shin.interaction.model.ReactionCount;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.Update;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class ReactionCountRepository {

    private final DynamoDbTemplate dynamoDbTemplate;

    public TransactWriteItem applyDelta(String videoId, Long likeDelta, Long deslikeDelta) {
        return TransactWriteItem.builder()
                .update(Update.builder()
                        .tableName("video_reaction_counters")
                        .key(Map.of(
                                "videoId", AttributeValue.builder().s(videoId).build()
                        ))
                        .updateExpression("ADD likesCount :likeDelta, deslikesCount :deslikeDelta")
                        .expressionAttributeValues(Map.of(
                                ":likeDelta", AttributeValue.builder().n(String.valueOf(likeDelta)).build(),
                                ":deslikeDelta", AttributeValue.builder().n(String.valueOf(deslikeDelta)).build()
                        ))
                        .build())
                .build();
    }

    public ReactionCount getCount(String videoId) {
        QueryConditional keyCondition = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(videoId).build());

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(keyCondition)
                .build();

        return dynamoDbTemplate.query(queryRequest, ReactionCount.class)
                .items()
                .stream()
                .findFirst()
                .orElse(ReactionCount.builder().videoId(videoId).likesCount(0L).deslikesCount(0L).build());
    }
}
