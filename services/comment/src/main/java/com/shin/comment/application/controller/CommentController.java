package com.shin.comment.application.controller;

import com.shin.comment.application.dto.CommentListResponse;
import com.shin.comment.application.dto.CreateCommentRequest;
import com.shin.comment.application.dto.CreateCommentResponse;
import com.shin.comment.application.dto.UpdateCommentRequest;
import com.shin.comment.application.dto.UpdateCommentResponse;
import com.shin.comment.domain.usecase.CreateCommentUseCase;
import com.shin.comment.domain.usecase.DeleteCommentUseCase;
import com.shin.comment.domain.usecase.ListCommentsUseCase;
import com.shin.comment.domain.usecase.UpdateCommentUseCase;
import com.shin.comment.infrastructure.openapi.docs.CommentControllerApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;


@RequiredArgsConstructor
@RequestMapping("${api.version}/comments")
@RestController
public class CommentController implements CommentControllerApi {

    private final CreateCommentUseCase createCommentUseCase;
    private final UpdateCommentUseCase updateCommentUseCase;
    private final DeleteCommentUseCase deleteCommentUseCase;
    private final ListCommentsUseCase listCommentsUseCase;

    @PostMapping
    public ResponseEntity<CreateCommentResponse> create(
            @RequestBody CreateCommentRequest createCommentRequest,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.status(201).body(createCommentUseCase.execute(userId.toString(), createCommentRequest));
    }

    @GetMapping
    public ResponseEntity<CommentListResponse> list(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String parentId,
            @RequestParam(defaultValue = "20") int maxResults,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "html") String textFormat
    ) {
        List<String> ids = (id != null && !id.isBlank())
                ? Arrays.asList(id.split(","))
                : List.of();

        return ResponseEntity.ok(listCommentsUseCase.execute(ids, parentId, maxResults, pageToken, textFormat));
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<UpdateCommentResponse> update(
            @RequestBody UpdateCommentRequest request,
            @PathVariable("commentId") UUID commentId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(updateCommentUseCase.execute(userId.toString(), commentId, request));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable("commentId") UUID commentId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        deleteCommentUseCase.execute(userId.toString(), commentId);
        return ResponseEntity.noContent().build();
    }
}
