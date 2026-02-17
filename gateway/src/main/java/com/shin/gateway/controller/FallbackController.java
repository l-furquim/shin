package com.shin.gateway.controller;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/metadata")
    public Mono<ProblemDetail> metadataFallback() {
        return createServiceUnavailableProblem("metadata-service", "Metadata service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/upload")
    public Mono<ProblemDetail> uploadFallback() {
        return createServiceUnavailableProblem("upload-service", "Upload service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/user")
    public Mono<ProblemDetail> userFallback() {
        return createServiceUnavailableProblem("user-service", "User service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/auth")
    public Mono<ProblemDetail> authFallback() {
        return createServiceUnavailableProblem("auth-service", "Auth service is temporarily unavailable. Please try again later.");
    }

    private Mono<ProblemDetail> createServiceUnavailableProblem(String service, String message) {
        return Mono.fromSupplier(() -> {
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                message
            );
            
            problemDetail.setType(URI.create("https://api.shin.com/errors/service-unavailable"));
            problemDetail.setTitle("Service Unavailable");
            
            String correlationId = MDC.get("correlationId");
            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString();
            }
            problemDetail.setProperty("correlationId", correlationId);
            problemDetail.setProperty("timestamp", Instant.now());
            problemDetail.setProperty("service", service);
            problemDetail.setProperty("status", "degraded");
            
            return problemDetail;
        });
    }
}
