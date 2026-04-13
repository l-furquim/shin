package com.shin.interaction.repository;

import com.shin.interaction.model.Reaction;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class ReactionRepository {

    private final DynamoDbTemplate dynamoDbTemplate;
    private final DynamoDbClient dynamoDbClient;

    public TransactWriteItem upsert(Reaction reaction) {
        String newType = reaction.getType().name().toLowerCase();
        return TransactWriteItem.builder()
                .put(Put.builder()
                        .tableName("video_reactions")
                        .item(Map.of(
                                "videoId", AttributeValue.builder().s(reaction.getVideoId().toString()).build(),
                                "userId", AttributeValue.builder().s(reaction.getUserId().toString()).build(),
                                "reactionType", AttributeValue.builder().s(newType).build()
                        ))
                        .conditionExpression("attribute_not_exists(videoId) OR reactionType <> :newType")
                        .expressionAttributeValues(Map.of(
                                ":newType", AttributeValue.builder().s(newType).build()
                        ))
                        .build())
                .build();
    }

    public Map<String, String> findReactionsByUserAndVideoIds(UUID userId, List<UUID> videoIds) {
        if (videoIds == null || videoIds.isEmpty()) {
            return Map.of();
        }

        List<Map<String, AttributeValue>> keys = videoIds.stream()
                .map(videoId -> Map.of(
                        "videoId", AttributeValue.builder().s(videoId.toString()).build(),
                        "userId", AttributeValue.builder().s(userId.toString()).build()
                ))
                .toList();

        KeysAndAttributes keysAndAttributes = KeysAndAttributes.builder()
                .keys(keys)
                .projectionExpression("videoId, reactionType")
                .build();

        BatchGetItemResponse response = dynamoDbClient.batchGetItem(r ->
                r.requestItems(Map.of("video_reactions", keysAndAttributes))
        );

        Map<String, String> result = new HashMap<>();
        List<Map<String, AttributeValue>> items = response.responses().get("video_reactions");
        if (items != null) {
            for (Map<String, AttributeValue> item : items) {
                String videoId = item.get("videoId").s();
                String reactionType = item.get("reactionType").s();
                result.put(videoId, reactionType);
            }
        }
        return result;
    }

    public TransactWriteItem delete(String videoId, String userId) {
        return TransactWriteItem.builder()
                .delete(Delete.builder()
                        .tableName("video_reactions")
                        .key(Map.of(
                                "videoId", AttributeValue.builder().s(videoId).build(),
                                "userId", AttributeValue.builder().s(userId).build()
                        ))
                        .conditionExpression("attribute_exists(videoId) AND attribute_exists(userId)")
                        .build())
                .build();
    }
}
