package com.shin.metadata.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.metadata.dto.CommentDeletedEvent;
import com.shin.metadata.repository.VideoRepository;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentDeletedConsumer {

    private final ObjectMapper objectMapper;
    private final VideoRepository videoRepository;

    @SqsListener(queueNames = "${spring.cloud.aws.queues.comment-deleted-queue}")
    @Transactional
    public void consume(String message, @Header("id") String messageId) {

        try {
            CommentDeletedEvent event = objectMapper.readValue(message, CommentDeletedEvent.class);
            UUID videoId = UUID.fromString(event.videoId());

            videoRepository.findById(videoId).ifPresent(video -> {
                video.setCommentCount(Math.max(0, video.getCommentCount() - 1));
                videoRepository.save(video);
            });
        } catch (Exception e) {
            log.error("Error consuming comment-deleted event messageId={}", messageId, e);
        }
    }

}
