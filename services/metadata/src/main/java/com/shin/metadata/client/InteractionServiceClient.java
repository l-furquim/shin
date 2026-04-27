package com.shin.metadata.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "interaction-service", url = "${clients.interaction-service.url}")
public interface InteractionServiceClient {

    @GetMapping("/api/v1/reactions/batch")
    Map<String, String> getBatchReactions(
            @RequestParam("videoIds") List<UUID> videoIds,
            @RequestHeader("X-User-Id") UUID userId
    );
}
