package com.shin.comment.domain.gateway;

import com.shin.comment.infrastructure.dto.ThreadCreatedEvent;

public interface ThreadMessageGateway {
    void sendThreadCreatedEvent(ThreadCreatedEvent threadCreatedEvent);
}
