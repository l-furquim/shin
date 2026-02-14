package com.shin.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/metadata")
    public Mono<ResponseEntity<Map<String, String>>> metadataFallback() {
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "message", "Metadata service is temporarily unavailable. Please try again later.",
                "service", "metadata-service",
                "status", "degraded"
            )));
    }

    @GetMapping("/upload")
    public Mono<ResponseEntity<Map<String, String>>> uploadFallback() {
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "message", "Upload service is temporarily unavailable. Please try again later.",
                "service", "upload-service",
                "status", "degraded"
            )));
    }

    @GetMapping("/user")
    public Mono<ResponseEntity<Map<String, String>>> userFallback() {
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "message", "User service is temporarily unavailable. Please try again later.",
                "service", "user-service",
                "status", "degraded"
            )));
    }
}
