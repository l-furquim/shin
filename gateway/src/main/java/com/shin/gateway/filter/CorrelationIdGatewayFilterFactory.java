package com.shin.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CorrelationIdGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdGatewayFilterFactory.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            HttpHeaders headers = exchange.getRequest().getHeaders();
            String correlationId = headers.getFirst(CORRELATION_ID_HEADER);

            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }

            final String finalCorrelationId = correlationId;
            
            exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, finalCorrelationId);

            return chain.filter(exchange)
                .doFirst(() -> {
                    MDC.put("correlationId", finalCorrelationId);
                    log.trace("Processing request with correlationId: {}", finalCorrelationId);
                })
                .doFinally(signalType -> MDC.clear());
        };
    }
}
