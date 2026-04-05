package com.shin.metadata.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DlqMonitoringConsumer {

    private final Counter dlqMessageCounter;

    public DlqMonitoringConsumer(MeterRegistry meterRegistry) {
        this.dlqMessageCounter = Counter.builder("sqs.dlq.messages")
                .description("Number of messages received in DLQs")
                .register(meterRegistry);
    }

    @SqsListener(queueNames = "${spring.cloud.aws.queues.encode-finished-dlq}")
    public void consumeEncodingDlq(String message, @Header("id") String messageId) {
        handleDlqMessage("encode-finished-dlq", messageId, message);
    }

    @SqsListener(queueNames = "${spring.cloud.aws.queues.thumbnail-finished-dlq}")
    public void consumeThumbnailDlq(String message, @Header("id") String messageId) {
        handleDlqMessage("thumbnail-finished-dlq", messageId, message);
    }

    @SqsListener(queueNames = "${spring.cloud.aws.queues.raw-upload-metadata-dlq}")
    public void consumeRawUploadDlq(String message, @Header("id") String messageId) {
        handleDlqMessage("raw-upload-metadata-dlq", messageId, message);
    }

    @SqsListener(queueNames = "${spring.cloud.aws.queues.view-count-dlq}")
    public void consumeViewCountDlq(String message, @Header("id") String messageId) {
        handleDlqMessage("view-count-dlq", messageId, message);
    }

    private void handleDlqMessage(String queueName, String messageId, String message) {
        dlqMessageCounter.increment();
        log.error("DLQ message received — queue={} messageId={} payload={}", queueName, messageId, message);
    }
}
