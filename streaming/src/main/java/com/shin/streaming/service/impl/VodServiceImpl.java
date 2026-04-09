package com.shin.streaming.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.shin.streaming.client.MetadataServiceClient;
import com.shin.streaming.config.CloudFrontConfig;
import com.shin.streaming.dto.VideoDetails;
import com.shin.streaming.dto.WatchVodResponse;
import com.shin.streaming.exception.VideoAccessDeniedException;
import com.shin.streaming.exception.VideoNotFoundException;
import com.shin.streaming.service.StorageService;
import com.shin.streaming.service.VodService;
import com.shin.streaming.dto.WatchVodResult;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudfront.cookie.CookiesForCustomPolicy;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class VodServiceImpl implements VodService {

    private final MetadataServiceClient metadataServiceClient;
    private final StorageService storageService;
    private final CloudFrontConfig cloudFrontConfig;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public WatchVodResult watchVod(UUID userId, UUID videoId, String videoUrl) {
        VideoDetails videoDetails = fetchVideoDetails(videoId);
        checkAccess(userId, videoDetails, videoUrl);

        UUID sessionId = UUID.randomUUID();
        CookiesForCustomPolicy signed = storageService.generateSignedCookiesForVideo(videoId);
        List<String> cookies = buildCookieHeaders(signed);
        String manifestUrl = "https://" + cloudFrontConfig.getCdnUrl() + "/" + videoId + "/master.m3u8";
        String playbackToken = generatePlaybackToken(sessionId, videoId, userId);

        return new WatchVodResult(new WatchVodResponse(videoDetails, manifestUrl, playbackToken), cookies);
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

    private String generatePlaybackToken(UUID sessionId, UUID videoId, UUID userId) {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        return JWT.create()
                .withIssuer("shin")
                .withSubject(userId != null ? userId.toString() : "anonymous")
                .withClaim("sessionId", sessionId.toString())
                .withClaim("videoId", videoId.toString())
                .withExpiresAt(Date.from(Instant.now().plusSeconds(cloudFrontConfig.getCookieValiditySeconds())))
                .sign(algorithm);
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
}
