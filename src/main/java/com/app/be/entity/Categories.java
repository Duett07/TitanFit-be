package com.app.be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
public class Categories extends BaseEntity {

    @OneToMany(mappedBy = "categories")
    private List<Product> products;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;
    private String categoryTree;
    private String imageUrl;
    private int sortOrder;
}
