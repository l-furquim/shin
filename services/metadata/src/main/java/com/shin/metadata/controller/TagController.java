package com.shin.metadata.controller;

import com.shin.metadata.dto.CreateTagRequest;
import com.shin.metadata.dto.ListTagsResponse;
import com.shin.metadata.dto.TagDto;
import com.shin.metadata.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.version}/tags")
public class TagController {

    private final TagService tagService;

    @GetMapping
    public ResponseEntity<ListTagsResponse> searchTags(
            @RequestParam(name = "q", required = false) String query
    ) {
        return ResponseEntity.ok(tagService.searchTags(query));
    }

    @PostMapping
    public ResponseEntity<TagDto> createTag(
            @Valid @RequestBody CreateTagRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagService.createTag(request));
    }
}
