package com.app.be.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.common.aliasing.qual.Unique;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
public class ProductVariant extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @OneToMany(mappedBy = "productVariant")
    private List<OrderItem> orderItems;

    @Unique
    private String sku;
    private String variantName;
    private BigDecimal price;
    private int stockQuantity;
}
