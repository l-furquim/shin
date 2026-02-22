package com.shin.user.service.impl;

import com.shin.user.dto.CreateSubscriptionResponse;
import com.shin.user.dto.GetCreatorSubscriptionsResponse;
import com.shin.user.dto.RemoveSubscriptionResponse;
import com.shin.user.exceptions.InvalidSubscriptionException;
import com.shin.user.model.Subscription;
import com.shin.user.model.SubscriptionId;
import com.shin.user.repository.CreatorRepository;
import com.shin.user.repository.SubscriptionRepository;
import com.shin.user.service.SubscriptionService;
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
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final CreatorRepository creatorRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String SUB_STATUS_KEY      = "sub:{userId}:{channelId}";
    private static final String SUB_COUNT_KEY       = "channel:{channelId}:sub:count";

    private static final Duration SUB_STATUS_TTL    = Duration.ofMinutes(10);
    private static final Duration SUB_COUNT_TTL     = Duration.ofMinutes(5);

    @Override
    @Transactional
    public CreateSubscriptionResponse subscribe(UUID userId, UUID channelId) {
        validateParams(userId, channelId);

        final var subId     = new SubscriptionId(userId, channelId);
        final var statusKey = buildStatusKey(userId, channelId);
        final var countKey  = buildCountKey(channelId);

        if (!isSubscribedResolved(subId, statusKey)) {
            subscriptionRepository.save(Subscription.builder().id(subId).build());
            creatorRepository.applySubscriberDelta(channelId, 1L);

            redisTemplate.opsForValue().set(statusKey, "true", SUB_STATUS_TTL);
            redisTemplate.delete(countKey); // invalidate stale count – will be reloaded
            log.info("User {} subscribed to channel {}", userId, channelId);
        }

        final long count = resolveCount(channelId, countKey);
        return new CreateSubscriptionResponse(true, count);
    }

    @Override
    @Transactional
    public RemoveSubscriptionResponse unsubscribe(UUID userId, UUID channelId) {
        validateParams(userId, channelId);

        final var subId     = new SubscriptionId(userId, channelId);
        final var statusKey = buildStatusKey(userId, channelId);
        final var countKey  = buildCountKey(channelId);

        if (isSubscribedResolved(subId, statusKey)) {
            subscriptionRepository.deleteById(subId);
            creatorRepository.applySubscriberDelta(channelId, -1L);

            redisTemplate.opsForValue().set(statusKey, "false", SUB_STATUS_TTL);
            redisTemplate.delete(countKey); // invalidate stale count – will be reloaded
            log.info("User {} unsubscribed from channel {}", userId, channelId);
        }

        final long count = resolveCount(channelId, countKey);
        return new RemoveSubscriptionResponse(false, count);
    }

    @Override
    public GetCreatorSubscriptionsResponse getSubscriptionInfo(UUID userId, UUID channelId) {
        validateParams(userId, channelId);

        final var subId     = new SubscriptionId(userId, channelId);
        final var statusKey = buildStatusKey(userId, channelId);
        final var countKey  = buildCountKey(channelId);

        final boolean subscribed = isSubscribedResolved(subId, statusKey);
        final long    count      = resolveCount(channelId, countKey);

        return new GetCreatorSubscriptionsResponse(subscribed, count);
    }

    private boolean isSubscribedResolved(SubscriptionId subId, String statusKey) {
        final String cached = redisTemplate.opsForValue().get(statusKey);

        if (cached != null) {
            return "true".equals(cached);
        }

        final boolean subscribed = subscriptionRepository.existsById(subId);
        redisTemplate.opsForValue().set(statusKey, subscribed ? "true" : "false", SUB_STATUS_TTL);
        return subscribed;
    }

    private long resolveCount(UUID channelId, String countKey) {
        final String cached = redisTemplate.opsForValue().get(countKey);

        if (cached != null) {
            try {
                return Long.parseLong(cached);
            } catch (NumberFormatException e) {
                log.warn("Malformed subscriber count in Redis for key {}: {}", countKey, cached);
                redisTemplate.delete(countKey);
            }
        }

        final long count = creatorRepository.findById(channelId)
                .map(c -> c.getSubscribersCount() != null ? c.getSubscribersCount() : 0L)
                .orElse(0L);

        redisTemplate.opsForValue().set(countKey, String.valueOf(count), SUB_COUNT_TTL);
        return count;
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
