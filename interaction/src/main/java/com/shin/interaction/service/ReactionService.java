package com.shin.interaction.service;

import com.shin.interaction.dto.CreateReactionResponse;
import com.shin.interaction.dto.DeleteReactionResponse;
import com.shin.interaction.model.REACTION_TYPE;

import java.util.UUID;

public interface ReactionService {

    CreateReactionResponse create(
            REACTION_TYPE type,
            UUID videoId,
            UUID userId
    );

    DeleteReactionResponse delete(
            REACTION_TYPE type,
            UUID videoId,
            UUID userId
    );



}
