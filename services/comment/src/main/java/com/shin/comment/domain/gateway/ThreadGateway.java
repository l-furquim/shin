package com.shin.comment.domain.gateway;

import com.shin.comment.domain.model.Thread;
import com.shin.comment.infrastructure.dto.ThreadQueryPage;
import com.shin.comment.infrastructure.dto.ThreadScanPage;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public interface ThreadGateway {

    Thread save(Thread thread);
    ThreadQueryPage findByVideoId(String videoId, int limit, Map<String, AttributeValue> exclusiveStartKey);
    ThreadScanPage findByIds(List<String> ids, int limit, Map<String, AttributeValue> exclusiveStartKey);
    ThreadScanPage findByChannelId(String channelId, int limit, Map<String, AttributeValue> exclusiveStartKey);

}
