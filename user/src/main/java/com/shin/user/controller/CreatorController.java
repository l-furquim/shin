package com.shin.user.controller;

import com.shin.user.dto.*;
import com.shin.user.service.CreatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("${api.version}/creators")
@RequiredArgsConstructor
public class CreatorController {

    private final CreatorService creatorService;

    @PostMapping
    public ResponseEntity<CreateCreatorResponse> create(
        @RequestPart(value = "avatar", required = false) MultipartFile avatar,
        @RequestPart(value = "banner", required = false) MultipartFile banner,
        @Valid @RequestPart("data") CreateCreatorRequest request,
        @RequestHeader("Accept-Language") String language
    ) {
        CreateCreatorResponse response = creatorService.createCreatorWithUser(
            avatar,
            banner,
            request,
            language
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UpdateCreatorResponse> update(
        @PathVariable UUID id,
        @RequestHeader("X-User-Id") UUID userId,
        @Valid @RequestPart(value = "data", required = false) UpdateCreatorRequest request,
        @RequestPart(value = "avatar", required = false) MultipartFile avatar,
        @RequestPart(value = "banner", required = false) MultipartFile banner
    ) {
        UpdateCreatorResponse response = creatorService.updateCreator(id, userId, request, avatar, banner);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetCreatorByIdResponse> getCreator(
        @PathVariable UUID id
    ){
         var response = creatorService.getCreatorById(id);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        creatorService.deleteCreator(id, userId);

        return ResponseEntity.noContent().build();
    }
}

