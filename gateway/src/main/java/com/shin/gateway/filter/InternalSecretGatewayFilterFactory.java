package com.shin.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class InternalSecretGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    @Value("${internal.secret}")
    private String internalSecret;

    public InternalSecretGatewayFilterFactory() {
        super(Object.class);
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .header(INTERNAL_SECRET_HEADER, internalSecret)
                    .build();
            return chain.filter(exchange.mutate().request(request).build());
        };
    }
}
