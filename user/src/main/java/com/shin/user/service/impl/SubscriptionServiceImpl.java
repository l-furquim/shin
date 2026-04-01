package com.shin.user.service.impl;

import com.shin.user.dto.CreateSubscriptionResponse;
import com.shin.user.dto.GetCreatorSubscriptionsResponse;
import com.shin.user.dto.RemoveSubscriptionResponse;
import com.shin.user.exceptions.InvalidSubscriptionException;
import com.shin.user.repository.CreatorRepository;
import com.shin.user.repository.SubscriptionRepository;
import com.shin.user.service.SubscriptionService;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.UUID;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SubscriptionServiceImpl.class);

    private final SubscriptionRepository subscriptionRepository;
    private final CreatorRepository creatorRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public SubscriptionServiceImpl(
            SubscriptionRepository subscriptionRepository,
            CreatorRepository creatorRepository,
            RedisTemplate<String, String> redisTemplate
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.creatorRepository = creatorRepository;
        this.redisTemplate = redisTemplate;
    }

    private static final String SUB_STATUS_KEY = "sub:{userId}:{channelId}";
    private static final String SUB_COUNT_KEY = "channel:{channelId}:sub:count";

    private static final Duration SUB_STATUS_TTL = Duration.ofMinutes(10);
    private static final Duration SUB_COUNT_TTL = Duration.ofMinutes(5);

    @Override
    @Transactional
    public CreateSubscriptionResponse subscribe(UUID userId, UUID channelId) {
        validateParams(userId, channelId);

        if (userId.equals(channelId)) {
            throw new InvalidSubscriptionException("User cannot subscribe to own channel.");
        }

        final var statusKey = buildStatusKey(userId, channelId);
        final var countKey = buildCountKey(channelId);
        final int inserted = subscriptionRepository.insertIgnore(userId, channelId);

        if (inserted > 0) {
            creatorRepository.applySubscriberDelta(channelId, 1L);
            LOGGER.info("User {} subscribed to channel {}", userId, channelId);
        }

        final long count = resolveCountFromDb(channelId);
        runAfterCommitOrNow(() -> {
            safeCacheWrite(() -> redisTemplate.opsForValue().set(statusKey, "true", SUB_STATUS_TTL));
            safeCacheWrite(() -> redisTemplate.opsForValue().set(countKey, String.valueOf(count), SUB_COUNT_TTL));
        });

        return new CreateSubscriptionResponse(true, count);
    }

    @Override
    @Transactional
    public RemoveSubscriptionResponse unsubscribe(UUID userId, UUID channelId) {
        validateParams(userId, channelId);

        final var statusKey = buildStatusKey(userId, channelId);
        final var countKey = buildCountKey(channelId);
        final int deleted = subscriptionRepository.deleteExisting(userId, channelId);

        if (deleted > 0) {
            creatorRepository.applySubscriberDelta(channelId, -1L);
            LOGGER.info("User {} unsubscribed from channel {}", userId, channelId);
        }

        final long count = resolveCountFromDb(channelId);
        runAfterCommitOrNow(() -> {
            safeCacheWrite(() -> redisTemplate.opsForValue().set(statusKey, "false", SUB_STATUS_TTL));
            safeCacheWrite(() -> redisTemplate.opsForValue().set(countKey, String.valueOf(count), SUB_COUNT_TTL));
        });

        return new RemoveSubscriptionResponse(false, count);
    }

    @Override
    public GetCreatorSubscriptionsResponse getSubscriptionInfo(UUID userId, UUID channelId) {
        validateParams(userId, channelId);

        final var statusKey = buildStatusKey(userId, channelId);
        final var countKey = buildCountKey(channelId);

        final boolean subscribed = isSubscribedResolved(userId, channelId, statusKey);
        final long count = resolveCount(channelId, countKey);

        return new GetCreatorSubscriptionsResponse(subscribed, count);
    }

    private boolean isSubscribedResolved(UUID userId, UUID channelId, String statusKey) {
        final String cached = redisTemplate.opsForValue().get(statusKey);

        if (cached != null) {
            return "true".equals(cached);
        }

        final boolean subscribed = subscriptionRepository.existsByIdFollowerIdAndIdChannelId(userId, channelId);
        redisTemplate.opsForValue().set(statusKey, subscribed ? "true" : "false", SUB_STATUS_TTL);
        return subscribed;
    }

    private long resolveCount(UUID channelId, String countKey) {
        final String cached = redisTemplate.opsForValue().get(countKey);

        if (cached != null) {
            try {
                return Long.parseLong(cached);
            } catch (NumberFormatException e) {
                LOGGER.warn("Malformed subscriber count in Redis for key {}: {}", countKey, cached);
                redisTemplate.delete(countKey);
            }
        }

        final long count = resolveCountFromDb(channelId);

        redisTemplate.opsForValue().set(countKey, String.valueOf(count), SUB_COUNT_TTL);
        return count;
    }

    private long resolveCountFromDb(UUID channelId) {
        final Long count = creatorRepository.findSubscribersCount(channelId);
        return count != null ? count : 0L;
    }

    private void runAfterCommitOrNow(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }

        action.run();
    }

    private void safeCacheWrite(Runnable cacheAction) {
        try {
            cacheAction.run();
        } catch (RuntimeException e) {
            LOGGER.warn("Cache operation failed: {}", e.getMessage());
        }
    }


    private String buildStatusKey(UUID userId, UUID channelId) {
        return SUB_STATUS_KEY
                .replace("{userId}", userId.toString())
                .replace("{channelId}", channelId.toString());
    }

    private String buildCountKey(UUID channelId) {
        return SUB_COUNT_KEY.replace("{channelId}", channelId.toString());
    }

    private void validateParams(UUID userId, UUID channelId) {
        if (userId == null || channelId == null) {
            throw new InvalidSubscriptionException("userId and channelId are required.");
        }
    }
}
