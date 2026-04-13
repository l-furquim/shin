package com.shin.streaming.service.impl;

import com.shin.streaming.client.MetadataServiceClient;
import com.shin.streaming.config.CloudFrontConfig;
import com.shin.streaming.dto.*;
import com.shin.streaming.exception.*;
import com.shin.streaming.producer.ViewCountProducer;
import com.shin.streaming.repository.VodSessionRepository;
import com.shin.streaming.service.AuthService;
import com.shin.streaming.service.StorageService;
import com.shin.streaming.service.VodService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudfront.cookie.CookiesForCustomPolicy;

import java.time.Instant;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class VodServiceImpl implements VodService {

    private static final List<String> RESOLUTIONS_ALLOWED = List.of("1080p", "720p", "480p", "360p");

    // A view qualifies when the user has watched at least this many seconds (for videos >= threshold)
    private static final long SHORT_VIDEO_THRESHOLD_SECONDS = 30;
    private static final double SHORT_VIDEO_MIN_RATIO = 0.75;
    private static final long LONG_VIDEO_MIN_WATCH_SECONDS = 30;

    private final MetadataServiceClient metadataServiceClient;
    private final StorageService storageService;
    private final AuthService authService;
    private final VodSessionRepository vodSessionRepository;
    private final ViewCountProducer viewCountProducer;
    private final CloudFrontConfig cloudFrontConfig;

    @Override
    public WatchVodResult watchVod(UUID userId, UUID videoId, String videoUrl, String[] resolutions) {
        checkResolutions(resolutions);
        VideoDetails videoDetails = fetchVideoDetails(videoId);
        checkAccess(userId, videoDetails, videoUrl);

        UUID sessionId = UUID.randomUUID();
        CookiesForCustomPolicy signed = storageService.generateSignedCookiesForVideo(videoId);
        List<String> cookies = buildCookieHeaders(signed);

        List<Map<String, String>> manifests = Arrays.stream(resolutions).map(r ->
                Map.of(r, "https://" + cloudFrontConfig.getCdnUrl() + "/videos/" + videoId + "/".concat(r).concat("/manifest.mpd"))
        ).toList();

        String playbackToken = authService.generatePlaybackToken(sessionId, videoId, userId);

        return new WatchVodResult(new WatchVodResponse(videoDetails, manifests, playbackToken), cookies);
    }

    @Override
    public void handlePlaybackEvent(ViewEventRequest request, UUID userId) {
        if (request.playbackSessionToken() == null || request.playbackSessionToken().isBlank()) {
            throw new VideoAccessDeniedException("Invalid playback session token");
        }

        final var jwt = authService.getToken(request.playbackSessionToken());
        if (jwt.getExpiresAt().before(Date.from(Instant.now()))) {
            throw new InvalidTokenException();
        }

        final var videoId    = jwt.getClaim("videoId").asString();
        final var sessionId  = jwt.getClaim("sessionId").asString();
        final var claimedUserId = jwt.getSubject();

        if (!claimedUserId.equals(userId.toString())) {
            throw new VideoAccessDeniedException("You cannot access this video");
        }

        VideoDetails videoDetails = fetchVideoDetails(UUID.fromString(videoId));

        if (videoDetails.visibility().equals("PRIVATE") && !videoDetails.creatorId().equals(userId)) {
            throw new VideoAccessDeniedException("You cannot access this video");
        }

        long videoDuration = videoDetails.duration() != null ? videoDetails.duration() : 0L;
        if (videoDuration <= 0) {
            log.warn("Video duration not available for videoId={}, skipping view qualification", videoId);
            return;
        }

        long newTotal = vodSessionRepository.accumulateWatchTime(
                sessionId, videoId, claimedUserId, request.watchTimeSeconds()
        );

        log.debug("Session {} accumulated {}s / {}s for videoId={}", sessionId, newTotal, videoDuration, videoId);

        if (isViewQualified(newTotal, videoDuration)) {
            boolean counted = vodSessionRepository.markViewCounted(sessionId);
            if (counted) {
                viewCountProducer.send(UUID.fromString(videoId), userId);
                log.info("View counted for videoId={} userId={} sessionId={}", videoId, userId, sessionId);
            }
        }
    }

    private boolean isViewQualified(long totalWatchSeconds, long videoDurationSeconds) {
        if (videoDurationSeconds >= SHORT_VIDEO_THRESHOLD_SECONDS) {
            return totalWatchSeconds >= LONG_VIDEO_MIN_WATCH_SECONDS;
        }
        long threshold = Math.max(1L, (long) (videoDurationSeconds * SHORT_VIDEO_MIN_RATIO));
        return totalWatchSeconds >= threshold;
    }

    private VideoDetails fetchVideoDetails(UUID videoId) {
        try {
            return metadataServiceClient.getWatchVideoById(videoId);
        } catch (FeignException.NotFound e) {
            throw new VideoNotFoundException("Video not found: " + videoId);
        }
    }

    private List<String> buildCookieHeaders(CookiesForCustomPolicy signed) {
        return List.of(
                withAttributes(signed.policyHeaderValue()),
                withAttributes(signed.signatureHeaderValue()),
                withAttributes(signed.keyPairIdHeaderValue())
        );
    }

    private String withAttributes(String nameValuePair) {
        return nameValuePair + "; Path=/; Secure; HttpOnly; SameSite=None";
    }

    private void checkAccess(UUID userId, VideoDetails video, String videoUrl) {
        switch (video.visibility()) {
            case "PUBLIC" -> { }
            case "NOT_LISTED" -> {
                if (videoUrl == null || videoUrl.isBlank()) {
                    throw new VideoAccessDeniedException("Access denied");
                }
            }
            case "PRIVATE" -> {
                if (userId == null || !userId.equals(video.creatorId())) {
                    throw new VideoAccessDeniedException("You do not have permission to access this video");
                }
            }
            default -> throw new VideoAccessDeniedException("Unknown visibility: " + video.visibility());
        }
    }

    private void checkResolutions(String[] resolutions) {
        for (String resolution : resolutions) {
            if (!RESOLUTIONS_ALLOWED.contains(resolution)) {
                throw new InvalidWatchVodRequest();
            }
        }
    }
}
