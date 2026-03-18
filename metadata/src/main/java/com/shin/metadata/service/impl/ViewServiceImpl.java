package com.shin.metadata.service.impl;

import com.shin.metadata.service.ViewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class ViewServiceImpl implements ViewService {

    private final RedisTemplate<String,Long> redisTemplate;

    private static final String VIEW_COUNT_KEY  = "videos:views";


    @Override
    public void increaseView(UUID videoId) {
        try {
            redisTemplate.opsForHash().increment(VIEW_COUNT_KEY, videoId, 1L);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public Optional<Long> getVideoViews(UUID videoId) {
        try {

            Object count = redisTemplate.opsForHash().get(VIEW_COUNT_KEY, videoId);

            return count == null ? Optional.empty() : Optional.of(Long.valueOf(count.toString()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            return Optional.empty();
        }
    }
}
