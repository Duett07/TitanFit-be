package com.app.be.repository;

import com.app.be.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderId(UUID orderId);

    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi WHERE oi.productVariant.product.id = :productId")
    boolean existsByProductVariantProductId(@Param("productId") UUID productId);
}
