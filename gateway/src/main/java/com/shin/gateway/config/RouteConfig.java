package com.shin.gateway.config;

import com.shin.gateway.filter.AuthContextGatewayFilterFactory;
import com.shin.gateway.filter.ClientIpResolverGatewayFilterFactory;
import com.shin.gateway.filter.CorrelationIdGatewayFilterFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
public class RouteConfig {

    @Value("${cloudfront.cdn-url:d1bdx17cpz5q2y.cloudfront.net}")
    private String cloudfrontCdnUrl;

    private final ClientIpResolverGatewayFilterFactory clientIpResolverFilter;
    private final CorrelationIdGatewayFilterFactory correlationIdFilter;
    private final AuthContextGatewayFilterFactory authContextFilter;

    private final KeyResolver ipKeyResolver;
    private final KeyResolver userKeyResolver;

    private final RedisRateLimiter authRateLimiter;
    private final RedisRateLimiter apiRateLimiter;
    private final RedisRateLimiter uploadRateLimiter;

    public RouteConfig(
        ClientIpResolverGatewayFilterFactory clientIpResolverFilter,
        CorrelationIdGatewayFilterFactory correlationIdFilter,
        AuthContextGatewayFilterFactory authContextFilter,
        @Qualifier("ipKeyResolver") KeyResolver ipKeyResolver,
        @Qualifier("userKeyResolver") KeyResolver userKeyResolver,
        @Qualifier("authRateLimiter") RedisRateLimiter authRateLimiter,
        @Qualifier("apiRateLimiter") RedisRateLimiter apiRateLimiter,
        @Qualifier("uploadRateLimiter") RedisRateLimiter uploadRateLimiter
    ) {
        this.clientIpResolverFilter = clientIpResolverFilter;
        this.correlationIdFilter = correlationIdFilter;
        this.authContextFilter = authContextFilter;
        this.ipKeyResolver = ipKeyResolver;
        this.userKeyResolver = userKeyResolver;
        this.authRateLimiter = authRateLimiter;
        this.apiRateLimiter = apiRateLimiter;
        this.uploadRateLimiter = uploadRateLimiter;
    }

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("cloudfront-cdn", r -> r
                        .path("/cdn/**")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .filter(clientIpResolverFilter.apply(new Object()))
                                .rewritePath("/cdn/(?<segment>.*)", "/${segment}")
                        )
                        .uri(cloudfrontUri())
                )

                .route("user-auth-public", r -> r
                        .path("/api/v1/users/auth")
                        .and()
                        .method(HttpMethod.POST)
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .filter(clientIpResolverFilter.apply(new Object()))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(authRateLimiter)
                                        .setKeyResolver(ipKeyResolver))
                                .circuitBreaker(config -> config
                                        .setName("userCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/user"))
                        )
                        .uri("lb://user-service")
                )

                .route("user-creators-create-public", r -> r
                        .path("/api/v1/creators")
                        .and()
                        .method(HttpMethod.POST)
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .filter(clientIpResolverFilter.apply(new Object()))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(authRateLimiter)
                                        .setKeyResolver(ipKeyResolver))
                                .circuitBreaker(config -> config
                                        .setName("userCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/user"))
                        )
                        .uri("lb://user-service")
                )

                .route("metadata-videos", r -> r
                        .path("/api/v1/videos/**")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .filter(clientIpResolverFilter.apply(new Object()))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(apiRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .filter(authContextFilter.apply(new Object()))
                                .circuitBreaker(config -> config
                                        .setName("metadataCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/metadata"))
                                .retry(retryConfig -> retryConfig.setRetries(3))
                        )
                        .uri("lb://metadata-service")
                )

                .route("metadata-tags", r -> r
                        .path("/api/v1/tags/**")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .filter(clientIpResolverFilter.apply(new Object()))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(apiRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .filter(authContextFilter.apply(new Object()))
                                .circuitBreaker(config -> config
                                        .setName("metadataCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/metadata"))
                                .retry(retryConfig -> retryConfig.setRetries(3))
                        )
                        .uri("lb://metadata-service")
                )

                .route("metadata-categories", r -> r
                        .path("/api/v1/categories/**")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .filter(clientIpResolverFilter.apply(new Object()))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(apiRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .filter(authContextFilter.apply(new Object()))
                                .circuitBreaker(config -> config
                                        .setName("metadataCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/metadata"))
                                .retry(retryConfig -> retryConfig.setRetries(3))
                        )
                        .uri("lb://metadata-service")
                )

                .route("metadata-playlists", r -> r
                        .path("/api/v1/playlists/**")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .filter(clientIpResolverFilter.apply(new Object()))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(apiRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .filter(authContextFilter.apply(new Object()))
                                .circuitBreaker(config -> config
                                        .setName("metadataCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/metadata"))
                        )
                        .uri("lb://metadata-service")
                )
                .route("upload-service", r -> r
                        .path("/api/v1/uploads/**")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .filter(clientIpResolverFilter.apply(new Object()))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(uploadRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .filter(authContextFilter.apply(new Object()))
                                .circuitBreaker(config -> config
                                        .setName("uploadCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/upload"))
                        )
                        .uri("lb://upload-service")
                )

                .route("user-users", r -> r
                        .path("/api/v1/users/**")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .filter(clientIpResolverFilter.apply(new Object()))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(apiRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .filter(authContextFilter.apply(new Object()))
                                .circuitBreaker(config -> config
                                        .setName("userCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/user"))
                        )
                        .uri("lb://user-service")
                )

                .route("user-creators", r -> r
                        .path("/api/v1/creators/**")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .filter(clientIpResolverFilter.apply(new Object()))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(apiRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .filter(authContextFilter.apply(new Object()))
                                .circuitBreaker(config -> config
                                        .setName("userCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/user"))
                        )
                        .uri("lb://user-service")
                )

                .route("auth", r -> r
                        .path("/api/v1/auth/**")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .filter(clientIpResolverFilter.apply(new Object()))
                                // More restrictive rate limiting for auth endpoints
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(authRateLimiter)
                                        .setKeyResolver(ipKeyResolver))
                                .circuitBreaker(config -> config
                                        .setName("authCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/auth"))
                        )
                        .uri("lb://auth-service")
                )

                .route("subscription-service", r -> r
                        .path("/api/v1/subscriptions/**")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .filter(clientIpResolverFilter.apply(new Object()))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(apiRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .filter(authContextFilter.apply(new Object()))
                                .circuitBreaker(config -> config
                                        .setName("subscriptionCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/subscription"))
                        )
                        .uri("lb://subscription-service")
                )

                .route("streaming-service-vod", r -> r
                        .path("/api/v1/vod", "/api/v1/vod/**")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .filter(clientIpResolverFilter.apply(new Object()))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(apiRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .filter(authContextFilter.apply(new Object()))
                                .circuitBreaker(config -> config
                                        .setName("streamingCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/streaming"))
                        )
                        .uri("lb://streaming-service")
                )

                .route("interaction-service-reactions", r -> r
                        .path("/api/v1/reactions/**")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .filter(clientIpResolverFilter.apply(new Object()))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(apiRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .filter(authContextFilter.apply(new Object()))
                                .circuitBreaker(config -> config
                                        .setName("interactionCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/interaction"))
                        )
                        .uri("lb://interaction-service")
                )

                .route("comment-service-comments", r -> r
                        .path("/api/v1/comments/**")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .filter(clientIpResolverFilter.apply(new Object()))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(apiRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .filter(authContextFilter.apply(new Object()))
                                .circuitBreaker(config -> config
                                        .setName("commentCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/comment"))
                        )
                        .uri("lb://comment-service")
                )


                .route("comment-service-threads", r -> r
                        .path("/api/v1/threads/**")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .filter(clientIpResolverFilter.apply(new Object()))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(apiRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .filter(authContextFilter.apply(new Object()))
                                .circuitBreaker(config -> config
                                        .setName("commentCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/comment"))
                        )
                        .uri("lb://comment-service")
                )

                .route("search-service", r -> r
                        .path("/api/v1/search/**")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .filter(clientIpResolverFilter.apply(new Object()))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(apiRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .filter(authContextFilter.apply(new Object()))
                                .circuitBreaker(config -> config
                                        .setName("searchCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/search"))
                        )
                        .uri("lb://search-service")
                )

                .route("search-service-docs", r -> r
                        .path("/search-service/v3/api-docs")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .rewritePath("/search-service/v3/api-docs", "/v3/api-docs"))
                        .uri("lb://search-service")
                )

                .route("subscription-service-docs", r -> r
                        .path("/subscription-service/v3/api-docs")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .rewritePath("/subscription-service/v3/api-docs", "/v3/api-docs"))
                        .uri("lb://subscription-service")
                )

                .route("comment-service-docs", r -> r
                        .path("/comment-service/v3/api-docs")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .rewritePath("/comment-service/v3/api-docs", "/v3/api-docs"))
                        .uri("lb://comment-service")
                )

                .route("interaction-service-docs", r -> r
                        .path("/interaction-service/v3/api-docs")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .rewritePath("/interaction-service/v3/api-docs", "/v3/api-docs"))
                        .uri("lb://interaction-service")
                )

                .route("metadata-service-docs", r -> r
                        .path("/metadata-service/v3/api-docs")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .rewritePath("/metadata-service/v3/api-docs", "/v3/api-docs"))
                        .uri("lb://metadata-service")
                )
                
                .route("upload-service-docs", r -> r
                        .path("/upload-service/v3/api-docs")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .rewritePath("/upload-service/v3/api-docs", "/v3/api-docs"))
                        .uri("lb://upload-service")
                )
                
                .route("user-service-docs", r -> r
                        .path("/user-service/v3/api-docs")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .rewritePath("/user-service/v3/api-docs", "/v3/api-docs"))
                        .uri("lb://user-service")
                )

                .route("auth-service-docs", r -> r
                        .path("/auth-service/v3/api-docs")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .rewritePath("/auth-service/v3/api-docs", "/v3/api-docs"))
                        .uri("lb://auth-service")
                )

                .route("streaming-service-docs", r -> r
                        .path("/streaming-service/v3/api-docs")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new Object()))
                                .rewritePath("/streaming-service/v3/api-docs", "/v3/api-docs"))
                        .uri("lb://streaming-service")
                )


                .build();
    }

    private String cloudfrontUri() {
        String normalized = cloudfrontCdnUrl == null ? "" : cloudfrontCdnUrl.trim();
        if (normalized.isEmpty()) {
            return "https://d1bdx17cpz5q2y.cloudfront.net";
        }

        normalized = normalized.replaceAll("/+$", "");
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }

        return "https://" + normalized;
    }
}
