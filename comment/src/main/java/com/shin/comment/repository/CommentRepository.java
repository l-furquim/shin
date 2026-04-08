package com.shin.comment.repository;

import com.shin.comment.model.Comment;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class CommentRepository {

    private static final String TABLE_NAME = "comments";

    private final DynamoDbTemplate dynamoDbTemplate;
    private final DynamoDbClient dynamoDbClient;

    public void save(Comment comment) {
        this.dynamoDbTemplate.save(comment);
    }

    public Optional<Comment> findById(String id) {
        QueryConditional keyCondition = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(id).build());

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(keyCondition)
                .build();

        return dynamoDbTemplate.query(queryRequest, Comment.class)
                .items()
                .stream()
                .findFirst();
    }

    public List<Comment> findByIds(List<String> ids) {
        if (ids.isEmpty()) return List.of();

        return ids.stream()
                .map(id -> {
                    QueryConditional keyCondition = QueryConditional
                            .keyEqualTo(Key.builder().partitionValue(id).build());
                    return dynamoDbTemplate.query(
                            QueryEnhancedRequest.builder().queryConditional(keyCondition).build(),
                            Comment.class)
                            .items()
                            .stream()
                            .findFirst();
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(c -> !c.isDeleted())
                .toList();
    }

    public CommentScanPage findByParentId(
            String parentId, int limit, Map<String, AttributeValue> exclusiveStartKey) {

        QueryRequest.Builder req = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .indexName("parentIdIndex")
                .keyConditionExpression("parentId = :pid")
                .expressionAttributeNames(Map.of("#del", "deleted"))
                .filterExpression("#del = :f")
                .expressionAttributeValues(Map.of(
                        ":pid", AttributeValue.fromS(parentId),
                        ":f", AttributeValue.fromBool(false)
                ))
                .limit(limit);

        if (exclusiveStartKey != null && !exclusiveStartKey.isEmpty()) {
            req.exclusiveStartKey(exclusiveStartKey);
        }

        QueryResponse response = dynamoDbClient.query(req.build());

        return new CommentScanPage(
                response.items().stream().map(this::toComment).toList(),
                response.lastEvaluatedKey()
        );
    }

    private Comment toComment(Map<String, AttributeValue> item) {
        return Comment.builder()
                .id(attrS(item, "id"))
                .createdAt(attrS(item, "createdAt"))
                .parentId(attrS(item, "parentId"))
                .videoId(attrS(item, "videoId"))
                .authorId(attrS(item, "authorId"))
                .textOriginal(attrS(item, "textOriginal"))
                .textDisplay(attrS(item, "textDisplay"))
                .likeCount(item.containsKey("likeCount")
                        ? Long.parseLong(item.get("likeCount").n()) : 0L)
                .deleted(item.containsKey("deleted") && Boolean.TRUE.equals(item.get("deleted").bool()))
                .updatedAt(attrS(item, "updatedAt"))
                .build();
    }

    private String attrS(Map<String, AttributeValue> item, String key) {
        return item.containsKey(key) ? item.get(key).s() : null;
    }
}
