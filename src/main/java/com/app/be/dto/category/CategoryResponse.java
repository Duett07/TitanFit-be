package com.app.be.dto.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private UUID id;
    private String name;
    private String slug;
    private String categoryTree;
    private String imageUrl;
    private int sortOrder;
    private long productCount;

    public CategoryResponse(UUID id, String name, String slug, String categoryTree, String imageUrl, int sortOrder, Long productCount) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.categoryTree = categoryTree;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
        this.productCount = productCount != null ? productCount : 0L;
    }
}
