package com.shin.subscription.service.impl;

import com.shin.commons.models.PageInfo;
import com.shin.subscription.dto.*;
import com.shin.subscription.exceptions.ForbiddenSubscriptionException;
import com.shin.subscription.exceptions.InvalidSubscriptionException;
import com.shin.subscription.exceptions.SubscriptionError;
import com.shin.subscription.producers.ChannelSubscribedProducer;
import com.shin.subscription.producers.ChannelUnsubscribedProducer;
import com.shin.subscription.repository.ChannelSubscriptionRepository;
import com.shin.subscription.repository.QueryPage;
import com.shin.subscription.service.SubscriptionCountService;
import com.shin.subscription.service.SubscriptionService;
import com.shin.subscription.service.UserSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import com.shin.subscription.model.ChannelSubscription;
import com.shin.subscription.model.UserSubscriptions;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final int MAX_LIMIT = 50;

    private final ChannelSubscriptionRepository channelSubscriptionRepository;
    private final SubscriptionCountService subscriptionCountService;
    private final UserSubscriptionService userSubscriptionService;

    private final ChannelSubscribedProducer channelSubscribedProducer;
    private final ChannelUnsubscribedProducer channelUnsubscribedProducer;

    private final DynamoDbClient dynamoDbClient;

    @Override
    public CreateSubscriptionResponse subscribe(UUID userId, UUID channelId) {
        if (userId.equals(channelId)) {
            throw new ForbiddenSubscriptionException();
        }

        var countTransaction = this.subscriptionCountService.buildDelta(channelId.toString(), 1L);
        var channelSubscriptionTransaction = this.channelSubscriptionRepository.buildWriteTransactionItem(channelId.toString(), userId.toString());
        var userSubscriptionTransaction = this.userSubscriptionService.buildTransaction(userId.toString(), channelId.toString());

        try {
            dynamoDbClient.transactWriteItems(r -> r.transactItems(
                    channelSubscriptionTransaction,
                    userSubscriptionTransaction,
                    countTransaction
            ));

        } catch (TransactionCanceledException e) {
            log.info("Invalid subscription {}, {}", channelId, e.getMessage());

            return null;
        } catch (Exception e) {
            log.error("Error while subscribing {}, {}", channelId, e.getMessage());

            // TODO: understand how to handle this exception properly.
            throw new SubscriptionError();
        }
        this.channelSubscribedProducer.sendEvent(channelId, userId);

        final var newCount = this.subscriptionCountService.getCurrentCount(channelId.toString());

        return new CreateSubscriptionResponse(
            true,
                newCount
        );
    }

    @Override
    public RemoveSubscriptionResponse unsubscribe(UUID userId, UUID channelId) {
        if (userId.equals(channelId)) {
            throw new ForbiddenSubscriptionException();
        }

        final var countTransaction = this.subscriptionCountService.buildDelta(channelId.toString(), -1L);
        final var channelSubscriptionTransaction = this.channelSubscriptionRepository.buildDeleteTransactionWrite(channelId.toString(), userId.toString());
        final var userSubscriptionTransaction = this.userSubscriptionService.buildDeleteTransaction(userId.toString(), channelId.toString());

        try {
            dynamoDbClient.transactWriteItems(r -> r.transactItems(
                    channelSubscriptionTransaction,
                    userSubscriptionTransaction,
                    countTransaction
            ));

        } catch (TransactionCanceledException e) {
            log.info("Invalid unsubscribed: {}, {}", channelId, e.getMessage());

            return null;
        } catch (Exception e) {
            log.error("Error while unsubscribing {}, {}", channelId, e.getMessage());

            throw new SubscriptionError();
        }

        final var newCount = this.subscriptionCountService.getCurrentCount(channelId.toString());

        this.channelUnsubscribedProducer.sendEvent(channelId, userId);

        return new RemoveSubscriptionResponse(
                false,
                newCount
        );
    }


    @Override
    public GetCreatorSubscriptionsResponse getSubscriptionInfo(UUID userId, UUID channelId) {
        final var count = this.subscriptionCountService.getCurrentCount(channelId.toString());

        var subscribed = userId.equals(channelId);

        if(!subscribed) {
            subscribed = this.userSubscriptionService.isUserSubscribed(userId.toString(), channelId.toString());
        }

        return new GetCreatorSubscriptionsResponse(subscribed, count);
    }

    @Override
    public GetSubscriptionsResponse getSubscriptions(GetSubscriptionsRequest request) {
        if (request.mySubscribers() && request.mine()) {
            throw new InvalidSubscriptionException("You can only choose between a mine subscriptions filter or a my subscribers filter");
        }

        if (request.myRecentSubscribers() && request.mine()) {
            throw new InvalidSubscriptionException("You can only use the my recent subscribers filter with the my subscribers filter");
        }

        if (request.channelId() != null && (request.mine() || request.myRecentSubscribers() || request.mySubscribers())) {
            throw new InvalidSubscriptionException("You cant use the channel id filter with other filters");
        }

        if (request.limit() < 1 || request.limit() > MAX_LIMIT) {
            throw new InvalidSubscriptionException("Limit must be between 1 and " + MAX_LIMIT);
        }

        if (request.channelId() != null) {
            return channelSubscriptionsResponse(channelSubscriptionRepository.findByChannelId(request.channelId().toString(), request.limit(), request.cursor()), request.limit());
        }

        if (request.myRecentSubscribers()) {
            return channelSubscriptionsResponse(channelSubscriptionRepository.findRecentByChannelId(request.userId().toString(), request.limit(), request.cursor()), request.limit());
        }

        if (request.mySubscribers()) {
            return channelSubscriptionsResponse(channelSubscriptionRepository.findByChannelId(request.userId().toString(), request.limit(), request.cursor()), request.limit());
        }

        if (request.mine()) {
            return userSubscriptionsResponse(userSubscriptionService.findByUserId(request.userId().toString(), request.limit(), request.cursor()), request.limit());
        }

        throw new InvalidSubscriptionException("You must use at least one filter");
    }

    private GetSubscriptionsResponse channelSubscriptionsResponse(QueryPage<ChannelSubscription> page, int limit) {
        List<SubscriptionDto> items = page.items().stream()
                .map(s -> new SubscriptionDto(s.getChannelId(), s.getUserId(), s.getCreatedAt()))
                .toList();
        return new GetSubscriptionsResponse(new PageInfo((long) items.size(), (long) limit), items, page.nextCursor());
    }

    private GetSubscriptionsResponse userSubscriptionsResponse(QueryPage<UserSubscriptions> page, int limit) {
        List<SubscriptionDto> items = page.items().stream()
                .map(s -> new SubscriptionDto(s.getChannelId(), s.getUserId(), null))
                .toList();
        return new GetSubscriptionsResponse(new PageInfo((long) items.size(), (long) limit), items, page.nextCursor());
    }
}
