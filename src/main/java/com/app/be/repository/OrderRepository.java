package com.app.be.repository;

import com.app.be.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderCode(String orderCode);

    boolean existsByOrderCode(String orderCode);

    Page<Order> findByUser_Id(UUID userId, Pageable pageable);

    List<Order> findByUser_Id(UUID userId);

    Page<Order> findByOrderStatus(String orderStatus, Pageable pageable);

    Page<Order> findByUser_IdAndOrderStatus(UUID userId, String orderStatus, Pageable pageable);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.productVariant"})
    List<Order> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    @EntityGraph(attributePaths = {"user", "orderItems", "orderItems.productVariant"})
    Optional<Order> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {"user", "orderItems", "orderItems.productVariant"})
    Optional<Order> findWithDetailsByOrderCode(String orderCode);
}
