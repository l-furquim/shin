package com.shin.comment.infrastructure.dto;

import com.shin.comment.domain.model.Thread;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public record ThreadScanPage(
        List<Thread> items,
        Map<String, AttributeValue> lastEvaluatedKey
) {}
