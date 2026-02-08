package com.shin.upload.producers;

import com.shin.upload.dto.ThumbnailJobEvent;
import com.shin.upload.exceptions.TranscoderProducerException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.awspring.cloud.sqs.operations.SqsTemplate;

@RequiredArgsConstructor
@Component
public class ThumbnailJobProducer {

    private final SqsTemplate sqsClient;

    @Value("${spring.cloud.aws.queues.thumbnail-job-queue}")
    private String queue;

    public void createJob(ThumbnailJobEvent event) {
        try {
            sqsClient.send(queue, event);
        } catch (Exception e) {
            throw new TranscoderProducerException(
                    e.getMessage() != null ? e.getMessage() : "Error while sending message to queue"
            );
        }
    }

}
