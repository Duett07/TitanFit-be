package com.app.be.repository.specification;

import com.app.be.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static final char ESCAPE_CHAR = '\\';
    public static final int MAX_KEYWORD_LENGTH = 100;

    public static Specification<Product> filterProducts(
            String keyword,
            String categorySlug,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter only active products on the public store
            predicates.add(cb.isTrue(root.get("isActive")));

            // Filter by keyword in product name with LIKE wildcards escaped
            if (StringUtils.hasText(keyword)) {
                String escapedKeyword = escapeLikePattern(keyword);
                if (StringUtils.hasText(escapedKeyword)) {
                    String pattern = "%" + escapedKeyword.toLowerCase() + "%";
                    predicates.add(cb.like(cb.lower(root.get("name")), pattern, ESCAPE_CHAR));
                }
            }

            // Filter by category slug
            if (StringUtils.hasText(categorySlug)) {
                predicates.add(cb.equal(root.get("categories").get("slug"), categorySlug.trim()));
            }

            // Filter by minPrice
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice));
            }

            // Filter by maxPrice
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Product> filterAdminProducts(
            String keyword,
            String categorySlug,
            Boolean isActive
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            if (StringUtils.hasText(keyword)) {
                String escapedKeyword = escapeLikePattern(keyword);
                if (StringUtils.hasText(escapedKeyword)) {
                    String pattern = "%" + escapedKeyword.toLowerCase() + "%";
                    predicates.add(cb.like(cb.lower(root.get("name")), pattern, ESCAPE_CHAR));
                }
            }

            if (StringUtils.hasText(categorySlug)) {
                predicates.add(cb.equal(root.get("categories").get("slug"), categorySlug.trim()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Escapes special wildcard characters for SQL LIKE query ('\', '%', '_')
     * and trims / limits length to MAX_KEYWORD_LENGTH characters to prevent
     * expensive full scans and wildcard denial of service.
     */
    public static String escapeLikePattern(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String clean = value.trim();
        if (clean.length() > MAX_KEYWORD_LENGTH) {
            clean = clean.substring(0, MAX_KEYWORD_LENGTH);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (c == ESCAPE_CHAR || c == '%' || c == '_') {
                sb.append(ESCAPE_CHAR);
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
