package com.shin.metadata.service.impl;

import com.shin.metadata.repository.VideoRepository;
import com.shin.metadata.service.ViewService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ViewServiceImpl implements ViewService {

    private static final Logger log = LoggerFactory.getLogger(ViewServiceImpl.class);

    private static final String PENDING_VIEWS_KEY = "videos:views:pending";
    private static final String VIEW_DEDUP_KEY = "video:view:dedupe:{videoId}:{viewerKey}";

    private final RedisTemplate<String, String> redisTemplate;
    private final VideoRepository videoRepository;
    private final Duration dedupeTtl;
    private final int flushBatchSize;

    public ViewServiceImpl(
            @Qualifier("stringTemplate") RedisTemplate<String, String> redisTemplate,
            VideoRepository videoRepository,
            @Value("${views.dedupe-ttl:PT1H}") Duration dedupeTtl,
            @Value("${views.flush-batch-size:500}") int flushBatchSize
    ) {
        this.redisTemplate = redisTemplate;
        this.videoRepository = videoRepository;
        this.dedupeTtl = dedupeTtl;
        this.flushBatchSize = flushBatchSize;
    }

    @Override
    public void increaseView(UUID videoId, String viewerKey) {
        if (videoId == null || viewerKey == null || viewerKey.isBlank()) {
            return;
        }

        String dedupeKey = buildDedupeKey(videoId, viewerKey);
        String field = videoId.toString();

        try {
            Boolean accepted = redisTemplate.opsForValue().setIfAbsent(dedupeKey, "1", dedupeTtl);
            if (!Boolean.TRUE.equals(accepted)) {
                return;
            }
            redisTemplate.opsForHash().increment(PENDING_VIEWS_KEY, field, 1L);
        } catch (Exception e) {
            log.error("Error while recording pending view for video {}", videoId, e);
            try {
                videoRepository.applyViewDelta(videoId, 1L);
            } catch (Exception dbException) {
                log.error("Error while applying fallback DB view increment for video {}", videoId, dbException);
            }
        }
    }

    @Override
    public Optional<Long> getPendingViews(UUID videoId) {
        if (videoId == null) {
            return Optional.empty();
        }

        try {
            Object count = redisTemplate.opsForHash().get(PENDING_VIEWS_KEY, videoId.toString());
            return count == null ? Optional.empty() : Optional.of(Long.valueOf(count.toString()));
        } catch (Exception e) {
            log.error("Error while getting pending views for video {}", videoId, e);
            return Optional.empty();
        }
    }

    @Override
    public long getEffectiveVideoViews(UUID videoId, Long persistedViews) {
        long base = persistedViews == null ? 0L : persistedViews;
        long pending = getPendingViews(videoId).orElse(0L);
        return Math.max(0L, base + pending);
    }

    @Override
    public Map<UUID, Long> getEffectiveVideoViews(Map<UUID, Long> persistedViewsByVideoId) {
        if (persistedViewsByVideoId == null || persistedViewsByVideoId.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<UUID> videoIds = persistedViewsByVideoId.keySet();
        Map<UUID, Long> pendingByVideoId = getPendingViews(videoIds);
        Map<UUID, Long> effectiveByVideoId = new HashMap<>(videoIds.size());

        for (UUID videoId : videoIds) {
            long base = persistedViewsByVideoId.getOrDefault(videoId, 0L) == null
                    ? 0L
                    : persistedViewsByVideoId.get(videoId);
            long pending = pendingByVideoId.getOrDefault(videoId, 0L);
            effectiveByVideoId.put(videoId, Math.max(0L, base + pending));
        }

        return effectiveByVideoId;
    }

    @Scheduled(fixedDelayString = "${views.flush-interval-ms:10000}")
    @Transactional
    public void flushPendingViewsToDatabase() {
        Map<Object, Object> pending;
        try {
            pending = redisTemplate.opsForHash().entries(PENDING_VIEWS_KEY);
        } catch (Exception e) {
            log.error("Error while reading pending views from Redis", e);
            return;
        }

        if (pending == null || pending.isEmpty()) {
            return;
        }

        int processed = 0;
        for (Map.Entry<Object, Object> entry : pending.entrySet()) {
            if (processed >= flushBatchSize) {
                break;
            }

            UUID videoId;
            long delta;

            try {
                videoId = UUID.fromString(String.valueOf(entry.getKey()));
                delta = Long.parseLong(String.valueOf(entry.getValue()));
            } catch (Exception e) {
                log.warn("Skipping malformed pending view entry key={} value={}", entry.getKey(), entry.getValue());
                continue;
            }

            if (delta <= 0L) {
                continue;
            }

            try {
                int updated = videoRepository.applyViewDelta(videoId, delta);
                if (updated > 0) {
                    Long remaining = redisTemplate.opsForHash().increment(PENDING_VIEWS_KEY, videoId.toString(), -delta);
                    if (remaining != null && remaining <= 0L) {
                        redisTemplate.opsForHash().delete(PENDING_VIEWS_KEY, videoId.toString());
                    }
                    processed++;
                } else {
                    redisTemplate.opsForHash().delete(PENDING_VIEWS_KEY, videoId.toString());
                }
            } catch (Exception e) {
                log.error("Error while flushing pending views for video {}", videoId, e);
            }
        }
    }

    private Map<UUID, Long> getPendingViews(Set<UUID> videoIds) {
        Map<UUID, Long> result = new HashMap<>(videoIds.size());
        if (videoIds.isEmpty()) {
            return result;
        }

        try {
            List<UUID> ids = new ArrayList<>(videoIds);
            List<String> fields = ids.stream().map(UUID::toString).toList();
            List<Object> values = redisTemplate.opsForHash().multiGet(PENDING_VIEWS_KEY, new ArrayList<>(fields));

            for (int i = 0; i < ids.size(); i++) {
                Object count = values != null && i < values.size() ? values.get(i) : null;
                long pending = count == null ? 0L : Long.parseLong(String.valueOf(count));
                UUID videoId = ids.get(i);
                result.put(videoId, Math.max(0L, pending));
            }
        } catch (Exception e) {
            log.error("Error while getting pending views in batch", e);
            for (UUID videoId : videoIds) {
                result.put(videoId, 0L);
            }
        }

        return result;
    }

    private String buildDedupeKey(UUID videoId, String viewerKey) {
        String hashedViewerKey = hashViewerKey(viewerKey);
        return VIEW_DEDUP_KEY
                .replace("{videoId}", videoId.toString())
                .replace("{viewerKey}", hashedViewerKey);
    }

    private String hashViewerKey(String viewerKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(viewerKey.trim().getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            return String.valueOf(viewerKey.hashCode());
        }
    }
}
