package com.app.be.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.common.aliasing.qual.Unique;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Categories categories;

    @OneToMany(mappedBy = "product")
    @org.hibernate.annotations.BatchSize(size = 20)
    private List<ProductVariant> productVariants;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews;

    private String name;
    @Unique
    private String slug;
    @Size
    @Column(columnDefinition = "LONGTEXT")
    private String description;
    private BigDecimal basePrice;
    private String thumbnailUrl;
    private boolean isActive;
}
