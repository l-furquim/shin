package com.shin.subscription.controller;

import com.shin.subscription.dto.CreateSubscriptionResponse;
import com.shin.subscription.dto.GetCreatorSubscriptionsResponse;
import com.shin.subscription.dto.RemoveSubscriptionResponse;
import com.shin.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.version}/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/{channelId}")
    public ResponseEntity<CreateSubscriptionResponse> subscribe(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable("channelId") UUID channelId
    ) {
        final var response = subscriptionService.subscribe(userId, channelId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{channelId}")
    public ResponseEntity<RemoveSubscriptionResponse> unsubscribe(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable("channelId") UUID channelId
    ) {
        final var response = subscriptionService.unsubscribe(userId, channelId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{channelId}/me")
    public ResponseEntity<GetCreatorSubscriptionsResponse> getSubscriptionInfo(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable("channelId") UUID channelId
    ) {
        final var response = subscriptionService.getSubscriptionInfo(userId, channelId);
        return ResponseEntity.ok(response);
    }
}
