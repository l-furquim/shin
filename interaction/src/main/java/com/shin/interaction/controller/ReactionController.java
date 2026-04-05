package com.shin.interaction.controller;

import com.shin.interaction.dto.CreateReactionResponse;
import com.shin.interaction.dto.DeleteReactionResponse;
import com.shin.interaction.model.REACTION_TYPE;
import com.shin.interaction.service.ReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.version}/reactions")
public class ReactionController {

    private final ReactionService reactionService;

   @PostMapping
   public ResponseEntity<CreateReactionResponse> interact(
           @RequestParam("type") String type,
           @RequestParam("videoId") UUID videoId,
           @RequestHeader("X-User-Id") UUID userId
   ) {
       final var reactionType = REACTION_TYPE.fromKey(type);

       if (reactionType.isEmpty()) {
           return ResponseEntity.badRequest().build();
       }

       final var response = this.reactionService.create(reactionType.get(), videoId, userId);

       return ResponseEntity.status(204).body(response);
   }

   @DeleteMapping
    public ResponseEntity<DeleteReactionResponse> delete(
           @RequestParam("type") String type,
           @RequestParam("videoId") UUID videoId,
           @RequestHeader("X-User-Id") UUID userId
   ) {

       final var reactionType = REACTION_TYPE.fromKey(type);

       if (reactionType.isEmpty()) {
           return ResponseEntity.badRequest().build();
       }

       final var response = this.reactionService.delete(reactionType.get(), videoId, userId);

       return ResponseEntity.ok().body(response);
   }

}
