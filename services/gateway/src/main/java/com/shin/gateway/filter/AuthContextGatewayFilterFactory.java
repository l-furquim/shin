package com.shin.gateway.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class AuthContextGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private final AuthContextFilter authContextFilter;

    @Override
    public GatewayFilter apply(Object config) {
        return authContextFilter;
    }
}
