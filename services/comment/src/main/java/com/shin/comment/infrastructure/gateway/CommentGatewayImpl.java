package com.shin.comment.infrastructure.gateway;

import com.shin.comment.domain.gateway.CommentGateway;
import com.shin.comment.domain.model.Comment;
import com.shin.comment.infrastructure.dto.CommentScanPage;
import com.shin.comment.infrastructure.entity.CommentEntity;
import com.shin.comment.infrastructure.mapper.CommentEntityMapper;
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
public class CommentGatewayImpl implements CommentGateway {

    private static final String TABLE_NAME = "comments";

    private final CommentEntityMapper commentEntityMapper;
    private final DynamoDbTemplate dynamoDbTemplate;
    private final DynamoDbClient dynamoDbClient;

    @Override
    public Comment save(Comment comment) {
        dynamoDbTemplate.save(commentEntityMapper.toEntity(comment));
        return comment;
    }

    @Override
    public Optional<Comment> findById(String id) {
        QueryConditional keyCondition = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(id).build());

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(keyCondition)
                .build();

        return dynamoDbTemplate.query(queryRequest, CommentEntity.class)
                .items()
                .stream()
                .findFirst()
                .map(commentEntityMapper::toDomain);
    }

    @Override
    public List<Comment> findByIds(List<String> ids) {
        if (ids.isEmpty()) return List.of();

        return ids.stream()
                .map(id -> {
                    QueryConditional keyCondition = QueryConditional
                            .keyEqualTo(Key.builder().partitionValue(id).build());
                    return dynamoDbTemplate.query(
                                    QueryEnhancedRequest.builder().queryConditional(keyCondition).build(),
                                    CommentEntity.class)
                            .items()
                            .stream()
                            .findFirst();
                })
                .filter(Optional::isPresent)
                .map(entity -> commentEntityMapper.toDomain(entity.get()))
                .filter(c -> !c.isDeleted())
                .toList();
    }

    @Override
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

        List<Comment> comments = response.items().stream()
                .map(this::toComment)
                .map(commentEntityMapper::toDomain)
                .toList();

        return new CommentScanPage(comments, response.lastEvaluatedKey());
    }

    private CommentEntity toComment(Map<String, AttributeValue> item) {
        return CommentEntity.builder()
                .id(attrS(item, "id"))
                .createdAt(attrS(item, "createdAt"))
                .parentId(attrS(item, "parentId"))
                .videoId(attrS(item, "videoId"))
                .authorId(attrS(item, "authorId"))
                .authorDisplayName(attrS(item, "authorDisplayName"))
                .authorAvatarUrl(attrS(item, "authorAvatarUrl"))
                .authorLink(attrS(item, "authorLink"))
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
