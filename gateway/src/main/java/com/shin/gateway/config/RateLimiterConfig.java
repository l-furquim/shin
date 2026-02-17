package com.shin.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;


@Configuration
public class RateLimiterConfig {

    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String clientIp = exchange.getRequest().getHeaders().getFirst("X-Client-IP");
            
            if (clientIp != null && !clientIp.isBlank()) {
                return Mono.just(clientIp);
            }
            
            var remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                return Mono.just(remoteAddress.getAddress().getHostAddress());
            }
            
            return Mono.just("unknown");
        };
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            
            String clientIp = exchange.getRequest().getHeaders().getFirst("X-Client-IP");
            if (clientIp != null && !clientIp.isBlank()) {
                return Mono.just("ip:" + clientIp);
            }
            
            return Mono.just("ip:unknown");
        };
    }

    @Bean
    public RedisRateLimiter authRateLimiter() {
        return new RedisRateLimiter(5, 10, 1);
    }

    @Bean
    public RedisRateLimiter apiRateLimiter() {
        return new RedisRateLimiter(50, 100, 1);
    }

    @Bean
    public RedisRateLimiter uploadRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }
}
