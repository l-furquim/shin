package com.shin.metadata.controller;

import com.shin.metadata.dto.CategoryDto;
import com.shin.metadata.dto.CreateCategoryRequest;
import com.shin.metadata.dto.ListCategoriesResponse;
import com.shin.metadata.service.VideoCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.version}/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final VideoCategoryService videoCategoryService;

    @GetMapping
    public ResponseEntity<ListCategoriesResponse> searchCategories(
            @RequestParam(name = "q", required = false) String query
    ) {
        return ResponseEntity.ok(videoCategoryService.searchCategories(query));
    }

    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(videoCategoryService.createCategory(request));
    }
}
