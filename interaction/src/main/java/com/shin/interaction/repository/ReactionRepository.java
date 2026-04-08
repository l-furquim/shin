package com.shin.interaction.repository;

import com.shin.interaction.model.Reaction;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class ReactionRepository {

    private final DynamoDbTemplate dynamoDbTemplate;

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
