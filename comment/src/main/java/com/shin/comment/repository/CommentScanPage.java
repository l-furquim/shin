package com.shin.comment.repository;

import com.shin.comment.model.Comment;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public record CommentScanPage(
        List<Comment> items,
        Map<String, AttributeValue> lastEvaluatedKey
) {}
