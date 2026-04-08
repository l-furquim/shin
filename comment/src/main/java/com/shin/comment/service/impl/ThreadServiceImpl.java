package com.shin.comment.service.impl;

import com.shin.comment.dto.ThreadDto;
import com.shin.comment.dto.ThreadListResponse;
import com.shin.comment.exception.InvalidCommentException;
import com.shin.comment.model.Thread;
import com.shin.comment.repository.ThreadQueryPage;
import com.shin.comment.repository.ThreadRepository;
import com.shin.comment.repository.ThreadScanPage;
import com.shin.comment.service.ThreadService;
import com.shin.commons.util.PageTokenUtil;
import com.shin.commons.models.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ThreadServiceImpl implements ThreadService {

    private final ThreadRepository threadRepository;

    @Override
    public void create(String topLevelComment, String authorId, String channelId, String videoId) {
        Thread thread = Thread.builder()
                .topLevelCommentId(topLevelComment)
                .authorId(authorId)
                .channelId(channelId)
                .videoId(videoId)
                .totalReplyCount(0L)
                .createdAt(LocalDateTime.now().toString())
                .updatedAt(LocalDateTime.now().toString())
                .build();

        this.threadRepository.save(thread);
    }

    @Override
    public ThreadListResponse listThreads(
            List<String> ids,
            String videoId,
            String channelId,
            int maxResults,
            String order,
            String pageToken) {

        boolean hasIds = ids != null && !ids.isEmpty();
        boolean hasVideoId = videoId != null && !videoId.isBlank();
        boolean hasChannelId = channelId != null && !channelId.isBlank();

        long filtersProvided = (hasIds ? 1 : 0) + (hasVideoId ? 1 : 0) + (hasChannelId ? 1 : 0);
        if (filtersProvided != 1) {
            throw new InvalidCommentException();
        }

        if (maxResults < 1 || maxResults > 100) {
            throw new InvalidCommentException();
        }

        Map<String, AttributeValue> startKey = null;
        if (pageToken != null && !pageToken.isBlank()) {
            startKey = PageTokenUtil.decode(pageToken).entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> AttributeValue.fromS(e.getValue())));
        }

        int fetchSize = maxResults + 1;
        List<Thread> threads;
        Map<String, AttributeValue> lastEvaluatedKey;

        if (hasVideoId) {
            ThreadQueryPage page = threadRepository.findByVideoId(videoId, fetchSize, startKey);
            threads = new ArrayList<>(page.items());
            lastEvaluatedKey = page.lastEvaluatedKey();
        } else if (hasIds) {
            ThreadScanPage page = threadRepository.findByIds(ids, fetchSize, startKey);
            threads = new ArrayList<>(page.items());
            lastEvaluatedKey = page.lastEvaluatedKey();
        } else {
            ThreadScanPage page = threadRepository.findByChannelId(channelId, fetchSize, startKey);
            threads = new ArrayList<>(page.items());
            lastEvaluatedKey = page.lastEvaluatedKey();
        }

        boolean hasMore = threads.size() > maxResults
                || (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty());

        if (threads.size() > maxResults) {
            threads = threads.subList(0, maxResults);
        }

        if ("time".equalsIgnoreCase(order)) {
            threads = threads.stream()
                    .sorted(Comparator.comparing(
                            t -> t.getCreatedAt() != null ? t.getCreatedAt() : "",
                            Comparator.reverseOrder()))
                    .toList();
        }

        String nextPageToken = null;
        if (hasMore && !threads.isEmpty()) {
            Thread last = threads.getLast();
            if (hasVideoId) {
                nextPageToken = PageTokenUtil.encode(
                        "videoId", last.getVideoId(),
                        "topLevelCommentId", last.getTopLevelCommentId()
                );
            } else if (hasChannelId) {
                nextPageToken = PageTokenUtil.encode(
                        "videoId", last.getVideoId(),
                        "topLevelCommentId", last.getTopLevelCommentId(),
                        "channelId", channelId
                );
            }
        }

        List<ThreadDto> items = threads.stream().map(this::toThreadDto).toList();

        return new ThreadListResponse(
                nextPageToken,
                new PageInfo((long) items.size(), (long) maxResults),
                items
        );
    }

    private ThreadDto toThreadDto(Thread t) {
        return new ThreadDto(
                t.getTopLevelCommentId(),
                t.getVideoId(),
                t.getChannelId(),
                t.getAuthorId(),
                t.getTotalReplyCount(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
