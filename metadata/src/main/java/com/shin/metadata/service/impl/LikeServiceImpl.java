package com.shin.metadata.service.impl;

import com.shin.metadata.dto.VideoLikesResponse;
import com.shin.metadata.exception.InvalidLikeException;
import com.shin.metadata.model.VideoLike;
import com.shin.metadata.model.VideoLikeId;
import com.shin.metadata.repository.LikeRepository;
import com.shin.metadata.repository.VideoRepository;
import com.shin.metadata.service.LikeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final VideoRepository videoRepository;
    private final RedisTemplate<String, String> redisTemplate;

    // like:{userId}:{videoId} -> "true" | "false"  (TTL: 10 min)
    private static final String LIKE_STATUS_KEY = "like:{userId}:{videoId}";
    // video:{videoId}:like:count -> Long as string  (TTL: 5 min)
    private static final String LIKE_COUNT_KEY  = "video:{videoId}:like:count";

    private static final Duration LIKE_STATUS_TTL = Duration.ofMinutes(10);
    private static final Duration LIKE_COUNT_TTL  = Duration.ofMinutes(5);

    @Override
    @Transactional
    public VideoLikesResponse like(UUID userId, UUID videoId) {
        validateParams(userId, videoId);

        final var likeId    = new VideoLikeId(videoId, userId);
        final var statusKey = buildStatusKey(userId, videoId);
        final var countKey  = buildCountKey(videoId);

        if (!isLikedResolved(likeId, statusKey)) {
            likeRepository.save(VideoLike.builder().id(likeId).build());
            videoRepository.applyLikeDelta(videoId, 1L);

            redisTemplate.opsForValue().set(statusKey, "true", LIKE_STATUS_TTL);
            redisTemplate.delete(countKey); // invalidate stale count
            log.info("User {} liked video {}", userId, videoId);
        }

        final long count = resolveCount(videoId, countKey);
        return new VideoLikesResponse(count, true);
    }

    @Override
    @Transactional
    public VideoLikesResponse unlike(UUID userId, UUID videoId) {
        validateParams(userId, videoId);

        final var likeId    = new VideoLikeId(videoId, userId);
        final var statusKey = buildStatusKey(userId, videoId);
        final var countKey  = buildCountKey(videoId);

        if (isLikedResolved(likeId, statusKey)) {
            likeRepository.deleteById(likeId);
            videoRepository.applyLikeDelta(videoId, -1L);

            redisTemplate.opsForValue().set(statusKey, "false", LIKE_STATUS_TTL);
            redisTemplate.delete(countKey); // invalidate stale count
            log.info("User {} unliked video {}", userId, videoId);
        }

        final long count = resolveCount(videoId, countKey);
        return new VideoLikesResponse(count, false);
    }

    @Override
    public VideoLikesResponse getLikeInfo(UUID userId, UUID videoId) {
        validateParams(userId, videoId);

        final var likeId    = new VideoLikeId(videoId, userId);
        final var statusKey = buildStatusKey(userId, videoId);
        final var countKey  = buildCountKey(videoId);

        final boolean liked = isLikedResolved(likeId, statusKey);
        final long    count = resolveCount(videoId, countKey);

        return new VideoLikesResponse(count, liked);
    }

    /**
     * Resolves whether {@code userId} has liked {@code videoId}.
     * Checks Redis first; on cache-miss falls back to the DB and warms the cache.
     */
    private boolean isLikedResolved(VideoLikeId likeId, String statusKey) {
        final String cached = redisTemplate.opsForValue().get(statusKey);

        if (cached != null) {
            return "true".equals(cached);
        }

        // Cache miss – query the DB and warm the cache
        final boolean liked = likeRepository.existsById(likeId);
        redisTemplate.opsForValue().set(statusKey, liked ? "true" : "false", LIKE_STATUS_TTL);
        return liked;
    }

    /**
     * Resolves the like count for {@code videoId}.
     * Checks Redis first; on cache-miss falls back to the Video read-model and warms the cache.
     */
    private long resolveCount(UUID videoId, String countKey) {
        final String cached = redisTemplate.opsForValue().get(countKey);

        if (cached != null) {
            try {
                return Long.parseLong(cached);
            } catch (NumberFormatException e) {
                log.warn("Malformed like count in Redis for key {}: {}", countKey, cached);
                redisTemplate.delete(countKey);
            }
        }

        // Cache miss – read the denormalised count from the videos table
        final long count = videoRepository.findById(videoId)
                .map(v -> v.getLikeCount() != null ? v.getLikeCount() : 0L)
                .orElse(0L);

        redisTemplate.opsForValue().set(countKey, String.valueOf(count), LIKE_COUNT_TTL);
        return count;
    }

    private String buildStatusKey(UUID userId, UUID videoId) {
        return LIKE_STATUS_KEY
                .replace("{userId}", userId.toString())
                .replace("{videoId}", videoId.toString());
    }

    private String buildCountKey(UUID videoId) {
        return LIKE_COUNT_KEY.replace("{videoId}", videoId.toString());
    }

    private void validateParams(UUID userId, UUID videoId) {
        if (userId == null || videoId == null) {
            throw new InvalidLikeException("userId and videoId are required.");
        }
    }
}
