package com.shin.comment.domain.gateway;

import com.shin.comment.domain.model.Comment;
import com.shin.comment.infrastructure.dto.CommentScanPage;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CommentGateway {

    Comment save(Comment comment);
    Optional<Comment> findById(String id);
    List<Comment> findByIds(List<String> ids);
    CommentScanPage findByParentId(String parentId, int limit, Map<String, AttributeValue> exclusiveStartKey);

}
