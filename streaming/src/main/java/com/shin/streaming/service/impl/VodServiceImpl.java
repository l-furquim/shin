package com.shin.streaming.service.impl;

import com.shin.streaming.client.MetadataServiceClient;
import com.shin.streaming.config.CloudFrontConfig;
import com.shin.streaming.dto.*;
import com.shin.streaming.exception.*;
import com.shin.streaming.producer.PlaybackProgressProducer;
import com.shin.streaming.service.AuthService;
import com.shin.streaming.service.StorageService;
import com.shin.streaming.service.VodService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudfront.cookie.CookiesForCustomPolicy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class VodServiceImpl implements VodService {

    private static final List<String> RESOLUTIONS_ALLOWED = List.of("1080p", "720p", "480p", "360p");

    private final MetadataServiceClient metadataServiceClient;
    private final StorageService storageService;
    private final AuthService authService;

    private final PlaybackProgressProducer playbackProgressProducer;

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

        return new WatchVodResult(new WatchVodResponse(videoDetails,manifests , playbackToken), cookies);
    }

    @Override
    public void handlePlaybackEvent(ViewEventRequest request, UUID userId) {
       if (request.playbackSessionToken() == null || request.playbackSessionToken().isBlank())  {
           throw new VideoAccessDeniedException("Invalid playback session token");
       }

       final var jwtDecoded = this.authService.getToken(request.playbackSessionToken());
       final var isExpired = jwtDecoded.getExpiresAt().before(Date.from(Instant.now()));

       if (isExpired) {
           throw new InvalidTokenException();
       }

        final var videoId = jwtDecoded.getClaim("videoId").asString();
        final var sessionId = jwtDecoded.getClaim("sessionId").asString();
        final var userClaimedId =  jwtDecoded.getSubject();

        if (!userClaimedId.equals(userId.toString())) {
            throw new VideoAccessDeniedException("You cannot access this video");
        }

        VideoDetails videoDetails = fetchVideoDetails(UUID.fromString(videoId));

        if (videoDetails.visibility().equals("PRIVATE") && !videoDetails.creatorId().equals(userId)) {
            throw new VideoAccessDeniedException("You cannot access this video");
        }

        if (!request.totalDurationSeconds().equals(videoDetails.duration())) {
            throw new InvalidPlackbayEvent();
        }

        this.playbackProgressProducer.sendEvent(new PlaybackProgressEvent(
                sessionId,
                videoId,
                userClaimedId,
                request.watchTimeSeconds(),
                request.currentPositionSeconds(),
                LocalDateTime.now()
        ));

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
            case "PUBLIC" -> {  }
            case "NOT_LISTED" -> {
                // TODO: Implement a more robust way, validating the signature of the url
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
