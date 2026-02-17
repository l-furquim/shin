package com.shin.gateway.config;

import com.shin.gateway.filter.AuthContextGatewayFilterFactory;
import com.shin.gateway.filter.ClientIpResolverGatewayFilterFactory;
import com.shin.gateway.filter.CorrelationIdGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

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
        KeyResolver ipKeyResolver,
        KeyResolver userKeyResolver,
        RedisRateLimiter authRateLimiter,
        RedisRateLimiter apiRateLimiter,
        RedisRateLimiter uploadRateLimiter
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
                
                .build();
    }
}
