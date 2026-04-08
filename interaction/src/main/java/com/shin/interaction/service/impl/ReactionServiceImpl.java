package com.shin.interaction.service.impl;

import com.shin.interaction.dto.CreateReactionResponse;
import com.shin.interaction.dto.DeleteReactionResponse;

import com.shin.interaction.exceptions.InvalidReactionException;
import com.shin.interaction.exceptions.ReactionTransactionException;
import com.shin.interaction.model.REACTION_TYPE;
import com.shin.interaction.model.Reaction;
import com.shin.interaction.producers.VideoDeslikedProducer;
import com.shin.interaction.producers.VideoLikedProducer;
import com.shin.interaction.repository.ReactionCountRepository;
import com.shin.interaction.repository.ReactionRepository;
import com.shin.interaction.service.ReactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReactionServiceImpl implements ReactionService {

    private final ReactionRepository reactionRepository;
    private final ReactionCountRepository reactionCountRepository;
    private final DynamoDbClient dynamoDbClient;
    private final VideoLikedProducer videoLikedProducer;
    private final VideoDeslikedProducer videoDeslikedProducer;

    @Override
    public CreateReactionResponse create(REACTION_TYPE type, UUID videoId, UUID userId) {
        if (videoId == null) {
            throw new InvalidReactionException();
        }

        final var reaction = Reaction.builder()
                .videoId(videoId)
                .userId(userId)
                .type(type)
                .build();

        if (type.equals(REACTION_TYPE.LIKE)) {
            boolean isNew = this.handleReaction(reaction, 1L, -1L);
            if (isNew) this.videoLikedProducer.sendEvent(videoId, userId);
        } else if (type.equals(REACTION_TYPE.DESLIKE)) {
            boolean isNew = this.handleReaction(reaction, -1L, 1L);
            if (isNew) this.videoDeslikedProducer.sendEvent(videoId, userId);
        }

        final var counts = reactionCountRepository.getCount(videoId.toString());

        return new CreateReactionResponse(
                counts.getLikesCount(),
                counts.getDeslikesCount(),
                LocalDateTime.now()
        );
    }

    @Override
    public DeleteReactionResponse delete(REACTION_TYPE type,UUID videoId, UUID userId) {
        if (videoId == null) {
            throw new InvalidReactionException();
        }

        try {
            final var deleteTransaction = reactionRepository.delete(videoId.toString(), userId.toString());

            long likeDelta = type == REACTION_TYPE.LIKE ? -1L : 0L;
            long dislikeDelta = type == REACTION_TYPE.DESLIKE ? -1L : 0L;

            final var counterTransaction = reactionCountRepository.applyDelta(videoId.toString(), likeDelta, dislikeDelta);

            dynamoDbClient.transactWriteItems(r -> r.transactItems(deleteTransaction, counterTransaction));

        } catch (TransactionCanceledException e) {
            log.info("Transaction failed for videoId={}, userId={}", videoId, userId);
        } catch (Exception e) {
            log.error("Delete reaction failed for videoId={}, userId={}: {}", videoId, userId, e.getMessage());

            throw new ReactionTransactionException();
        }

        final var counts = reactionCountRepository.getCount(videoId.toString());
        return new DeleteReactionResponse(counts.getLikesCount(), counts.getDeslikesCount());
    }

    private boolean handleReaction(Reaction reaction, Long likeDelta, Long deslikeDelta) {
        try {
            final var upsertTransaction = reactionRepository.upsert(reaction);
            final var counterTransaction = reactionCountRepository.applyDelta(
                    reaction.getVideoId().toString(), likeDelta, deslikeDelta
            );

            dynamoDbClient.transactWriteItems(r -> r.transactItems(
                    upsertTransaction,
                    counterTransaction
            ));

            return true;

        } catch (TransactionCanceledException e) {
            log.debug("Duplicate reaction ignored for videoId={}, userId={}", reaction.getVideoId(), reaction.getUserId());
            return false;
        } catch (Exception e) {
            log.error("Reaction transaction failed for videoId={}, userId={}: {}",
                    reaction.getVideoId(), reaction.getUserId(), e.getMessage());
            throw new ReactionTransactionException();
        }
    }
}
