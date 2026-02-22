package com.shin.interaction.service.impl;

import com.shin.interaction.dto.DeleteLikeResponse;
import com.shin.interaction.dto.GetVideoLikesResponse;
import com.shin.interaction.exception.InvalidLikeException;
import com.shin.interaction.repository.LikeRepository;
import com.shin.interaction.service.LikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final RedisTemplate<String, String> videoLikesRedisTemplate;
    private final RedisTemplate<String, UUID> userLikeRedisTemplate;

    private static final String USER_LIKES_PREFIX = "video:{userId}:likes:count";
    private static final String VIDEO_LIKES_USER_PREFIX = "video:{videoId}:likes:users";

    @Override
    public void create(UUID userId, UUID videoId) {
        if(userId == null || videoId == null){
            throw new InvalidLikeException("Please, provide all data required.");
        }

        try {
            final var userKey = USER_LIKES_PREFIX.replace("{userId}", userId.toString());
            final var videoKey = VIDEO_LIKES_USER_PREFIX.replace("{videoId}", videoId.toString());

            final var userAlreadyLiked = userLikeRedisTemplate
                    .opsForSet()
                    .isMember(userKey,  userId);

            if(Boolean.TRUE.equals(userAlreadyLiked)){
               return;
            }

            userLikeRedisTemplate.opsForSet().add(USER_LIKES_PREFIX.replace("{userId}", userId.toString()), videoId);

            videoLikesRedisTemplate.opsForValue().increment(
                    videoKey,
                    1
            );
        } catch (Exception e){
            log.error("Error while liking a video: {}", e.getMessage());
        }

    }

    @Override
    public GetVideoLikesResponse get(UUID userId, UUID videoId) {
        if(userId == null || videoId == null){
            throw new InvalidLikeException("Please, provide all data required.");
        }

        try {

            final var userKey = USER_LIKES_PREFIX.replace("{userId}", userId.toString());
            final var videoKey = VIDEO_LIKES_USER_PREFIX.replace("{videoId}", videoId.toString());

            final var userLiked =  userLikeRedisTemplate.opsForSet().isMember(userKey,  userId);
            final var likes =  videoLikesRedisTemplate.opsForSet().size(videoKey);


            return new GetVideoLikesResponse(likes, Boolean.TRUE.equals(userLiked));
        } catch (Exception e ) {
            log.error("Error while getting likes from the video: {}, {}", videoId, e.getMessage());

            throw e;
        }

    }

    @Override
    public DeleteLikeResponse delete(UUID userId, UUID videoId) {
        if(userId == null || videoId == null){
            throw new InvalidLikeException("Please, provide all data required.");
        }

        try {
            final var userKey = USER_LIKES_PREFIX.replace("{userId}", userId.toString());
            final var videoKey = VIDEO_LIKES_USER_PREFIX.replace("{videoId}", videoId.toString());

            final var userAlreadyLiked = userLikeRedisTemplate
                    .opsForSet()
                    .isMember(userKey,  userId);

            if(Boolean.TRUE.equals(userAlreadyLiked)){
                userLikeRedisTemplate.opsForSet().remove(USER_LIKES_PREFIX.replace("{userId}", userId.toString()), videoId);

                videoLikesRedisTemplate.opsForValue().decrement(
                        videoKey,
                        1
                );
            }

            final var likes = videoLikesRedisTemplate.opsForSet().size(videoKey);

            return new DeleteLikeResponse(likes, false);
        } catch (Exception e){
            log.error("Error while deleting a like: {}", e.getMessage());

            throw e;
        }
    }
}
