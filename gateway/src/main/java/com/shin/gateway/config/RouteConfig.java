package com.shin.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("metadata-videos", r -> r
                        .path("/api/v1/videos/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("metadataCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/metadata")
                                )
                                .retry(retryConfig -> retryConfig.setRetries(3))
                        )
                        .uri("lb://metadata-service")
                )
                
                .route("metadata-playlists", r -> r
                        .path("/api/v1/playlists/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("metadataCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/metadata")
                                )
                        )
                        .uri("lb://metadata-service")
                )
                
                .route("upload-service", r -> r
                        .path("/api/v1/uploads/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("uploadCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/upload")
                                )
                        )
                        .uri("lb://upload-service")
                )
                
                .route("user-users", r -> r
                        .path("/api/v1/users/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("userCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/user")
                                )
                        )
                        .uri("lb://user-service")
                )
                
                .route("user-creators", r -> r
                        .path("/api/v1/creators/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("userCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/user")
                                )
                        )
                        .uri("lb://user-service")
                )
                
                .build();
    }
}
