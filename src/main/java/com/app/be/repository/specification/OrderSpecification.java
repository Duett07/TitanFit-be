package com.app.be.repository.specification;

import com.app.be.entity.Order;
import com.app.be.enums.OrderStatus;
import com.app.be.enums.PaymentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderSpecification {

    public static Specification<Order> filterOrders(
            UUID userId,
            OrderStatus status,
            PaymentStatus paymentStatus,
            LocalDate startDate,
            LocalDate endDate,
            String search
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("orderStatus"), status));
            }

            if (paymentStatus != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), paymentStatus));
            }

            if (startDate != null) {
                LocalDateTime startDateTime = startDate.atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDateTime));
            }

            if (endDate != null) {
                LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDateTime));
            }

            if (StringUtils.hasText(search)) {
                String cleanSearch = ProductSpecification.escapeLikePattern(search);
                if (StringUtils.hasText(cleanSearch)) {
                    String pattern = "%" + cleanSearch.toLowerCase() + "%";
                    Predicate codeMatch = cb.like(cb.lower(root.get("orderCode")), pattern, ProductSpecification.ESCAPE_CHAR);
                    Predicate customerMatch = cb.like(cb.lower(root.get("customerName")), pattern, ProductSpecification.ESCAPE_CHAR);
                    Predicate phoneMatch = cb.like(cb.lower(root.get("phone")), pattern, ProductSpecification.ESCAPE_CHAR);
                    predicates.add(cb.or(codeMatch, customerMatch, phoneMatch));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
