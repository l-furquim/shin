package com.shin.streaming.service.impl;

import com.shin.streaming.config.CloudFrontConfig;
import com.shin.streaming.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;
import software.amazon.awssdk.services.cloudfront.cookie.CookiesForCustomPolicy;
import software.amazon.awssdk.services.cloudfront.model.CustomSignerRequest;

import java.security.PrivateKey;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final CloudFrontConfig cloudFrontConfig;
    private final CloudFrontUtilities cloudFrontUtilities;
    private final PrivateKey cloudFrontPrivateKey;

    @Override
    public CookiesForCustomPolicy generateSignedCookiesForVideo(UUID videoId) {
        try {
            String resourceUrl = "https://" + cloudFrontConfig.getCdnUrl() + "/" + videoId + "/*";
            Instant expiresAt = Instant.now().plusSeconds(cloudFrontConfig.getCookieValiditySeconds());

            CustomSignerRequest request = CustomSignerRequest.builder()
                    .resourceUrl(resourceUrl)
                    .privateKey(cloudFrontPrivateKey)
                    .keyPairId(cloudFrontConfig.getKeyPairId())
                    .expirationDate(expiresAt)
                    .build();

            return cloudFrontUtilities.getCookiesForCustomPolicy(request);
        } catch (Exception e) {
            log.error("Failed to generate signed cookies for video {}", videoId, e);
            throw new RuntimeException("Failed to generate CloudFront signed cookies", e);
        }
    }
}
