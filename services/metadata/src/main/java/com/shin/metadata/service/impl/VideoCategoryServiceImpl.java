package com.shin.metadata.service.impl;

import com.shin.metadata.dto.CategoryDto;
import com.shin.metadata.dto.CreateCategoryRequest;
import com.shin.metadata.dto.ListCategoriesResponse;
import com.shin.metadata.exception.InvalidVideoRequestException;
import com.shin.metadata.model.VideoCategory;
import com.shin.metadata.repository.VideoCategoryRepository;
import com.shin.metadata.service.VideoCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoCategoryServiceImpl implements VideoCategoryService {

    private final VideoCategoryRepository videoCategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public ListCategoriesResponse searchCategories(String query) {
        List<VideoCategory> categories = query != null && !query.isBlank()
                ? videoCategoryRepository.findByNameContainingIgnoreCase(query)
                : videoCategoryRepository.findAll();
        List<CategoryDto> items = categories.stream()
                .map(c -> new CategoryDto(c.getId(), c.getName()))
                .toList();
        return new ListCategoriesResponse(items);
    }

    @Override
    @Transactional
    public CategoryDto createCategory(CreateCategoryRequest request) {
        VideoCategory category = VideoCategory.builder()
                .name(request.name())
                .build();
        category = videoCategoryRepository.save(category);
        return new CategoryDto(category.getId(), category.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public VideoCategory findCategoryOrThrow(Long id) {
        return videoCategoryRepository.findById(id)
                .orElseThrow(() -> new InvalidVideoRequestException("Category with ID " + id + " not found"));
    }
}
