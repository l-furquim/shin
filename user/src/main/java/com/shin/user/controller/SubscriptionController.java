package com.shin.user.controller;

import com.shin.user.dto.CreateSubscriptionResponse;
import com.shin.user.dto.GetCreatorSubscriptionsResponse;
import com.shin.user.dto.RemoveSubscriptionResponse;
import com.shin.user.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.version}/creators")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/{channelId}/subscriptions")
    public ResponseEntity<CreateSubscriptionResponse> subscribe(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable("channelId") UUID channelId
    ) {
        final var response = subscriptionService.subscribe(userId, channelId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{channelId}/subscriptions")
    public ResponseEntity<RemoveSubscriptionResponse> unsubscribe(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable("channelId") UUID channelId
    ) {
        final var response = subscriptionService.unsubscribe(userId, channelId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{channelId}/subscriptions/me")
    public ResponseEntity<GetCreatorSubscriptionsResponse> getSubscriptionInfo(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable("channelId") UUID channelId
    ) {
        final var response = subscriptionService.getSubscriptionInfo(userId, channelId);
        return ResponseEntity.ok(response);
    }
}
