package com.shin.metadata.dto;

import java.util.List;

public record ListCategoriesResponse(
        List<CategoryDto> items
) {}
