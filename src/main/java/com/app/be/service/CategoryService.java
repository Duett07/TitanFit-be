package com.app.be.service;

import com.app.be.dto.category.CategoryResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAllCategories();
}
