package com.shin.comment.infrastructure.dto;

import com.shin.comment.domain.model.Comment;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public record CommentScanPage(
        List<Comment> items,
        Map<String, AttributeValue> lastEvaluatedKey
) {}
