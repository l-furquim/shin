package com.shin.subscription.controller;

import com.shin.subscription.dto.CreateSubscriptionResponse;
import com.shin.subscription.dto.GetSubscriptionsRequest;
import com.shin.subscription.dto.GetSubscriptionsResponse;
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


    @GetMapping
    public ResponseEntity<GetSubscriptionsResponse> getSubscription(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(value = "channelId", required = false ) UUID channelId,
            @RequestParam(value = "myRecentSubscribers", required = false, defaultValue = "false") Boolean myRecentSubscribers,
            @RequestParam(value = "mine", required = false,defaultValue="false") Boolean mine,
            @RequestParam(value = "mySubscribers", required = false ,defaultValue = "false") Boolean mySubscribers,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit
    ) {
        final var request = new GetSubscriptionsRequest(
                userId,
                channelId,
                myRecentSubscribers,
                mine,
                mySubscribers,
                cursor,
                limit
        );

        final var response = subscriptionService.getSubscriptions(request);

        return ResponseEntity.ok(response);
    }
}
