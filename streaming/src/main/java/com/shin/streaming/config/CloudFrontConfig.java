package com.shin.streaming.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "cloudfront")
@Getter
@Setter
public class CloudFrontConfig {

    private String cdnUrl;
    private String keyPairId;
    private String privateKeySecretArn;
    private long cookieValiditySeconds = 3600;

    @Bean
    public CloudFrontUtilities cloudFrontUtilities() {
        return CloudFrontUtilities.create();
    }

    @Bean
    public PrivateKey cloudFrontPrivateKey() {
        log.info("Loading CloudFront private key from Secrets Manager: {}", privateKeySecretArn);

        try (SecretsManagerClient client = SecretsManagerClient.builder()
                .region(Region.US_EAST_1)
                .build()) {

            GetSecretValueResponse response = client.getSecretValue(
                    GetSecretValueRequest.builder()
                            .secretId(privateKeySecretArn)
                            .build()
            );

            return parsePemPrivateKey(response.secretString());
        }
    }

    private PrivateKey parsePemPrivateKey(String pem) {
        try {
            String stripped = pem
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(stripped);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse CloudFront private key from PEM", e);
        }
    }
}
