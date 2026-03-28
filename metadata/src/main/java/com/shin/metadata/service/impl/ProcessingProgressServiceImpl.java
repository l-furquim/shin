package com.shin.metadata.service.impl;

import com.shin.metadata.dto.ProcessingProgress;
import com.shin.metadata.dto.UploadState;
import com.shin.metadata.service.ProcessingProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class ProcessingProgressServiceImpl implements ProcessingProgressService {

    private static final String KEY_PREFIX = "upload:";

    private final RedisTemplate<String, UploadState> redisTemplate;

    public ProcessingProgressServiceImpl(@Qualifier("uploadTemplate") RedisTemplate<String, UploadState> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ProcessingProgress getProgress(UUID videoId) {
        try {
            UploadState value = redisTemplate.opsForValue().get(videoId.toString());

            return value == null ? null : new ProcessingProgress(
                    value.totalChunks(),
                    value.uploadedChunks()
            );
        } catch (Exception e) {
            log.error("Failed to get processing progress for video {}: {}", videoId, e.getMessage());
            return null;
        }
    }

    @Override
    public void clearProgress(UUID videoId) {
        try {
            redisTemplate.delete(key(videoId));
        } catch (Exception e) {
            log.error("Failed to clear processing progress for video {}: {}", videoId, e.getMessage());
        }
    }

    private String key(UUID videoId) {
        return KEY_PREFIX + videoId;
    }
}
