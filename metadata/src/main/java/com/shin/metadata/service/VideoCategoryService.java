package com.shin.metadata.service;

import com.shin.metadata.dto.CategoryDto;
import com.shin.metadata.dto.CreateCategoryRequest;
import com.shin.metadata.dto.ListCategoriesResponse;
import com.shin.metadata.model.VideoCategory;

public interface VideoCategoryService {
    ListCategoriesResponse searchCategories(String query);
    CategoryDto createCategory(CreateCategoryRequest request);
    VideoCategory findCategoryOrThrow(Long id);
}
