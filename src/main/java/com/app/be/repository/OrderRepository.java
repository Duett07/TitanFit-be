package com.app.be.repository;

import com.app.be.entity.Order;
import com.app.be.enums.OrderStatus;
import com.app.be.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderCode(String orderCode);

    boolean existsByOrderCode(String orderCode);

    Page<Order> findByUser_Id(UUID userId, Pageable pageable);

    List<Order> findByUser_Id(UUID userId);

    Page<Order> findByOrderStatus(OrderStatus orderStatus, Pageable pageable);

    Page<Order> findByUser_IdAndOrderStatus(UUID userId, OrderStatus orderStatus, Pageable pageable);

    Page<Order> findByUser_IdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Order> findByUser_IdAndOrderStatusOrderByCreatedAtDesc(UUID userId, OrderStatus orderStatus, Pageable pageable);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.productVariant"})
    List<Order> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    @EntityGraph(attributePaths = {"user", "orderItems", "orderItems.productVariant"})
    List<Order> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"user", "orderItems", "orderItems.productVariant"})
    Optional<Order> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {"user", "orderItems", "orderItems.productVariant"})
    Optional<Order> findWithDetailsByOrderCode(String orderCode);

    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<Order> findAll(Specification<Order> spec, Pageable pageable);

    long countByOrderStatus(OrderStatus orderStatus);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderStatus = :orderStatus")
    BigDecimal sumTotalAmountByOrderStatus(@Param("orderStatus") OrderStatus orderStatus);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.paymentStatus = :paymentStatus")
    BigDecimal sumTotalAmountByPaymentStatus(@Param("paymentStatus") PaymentStatus paymentStatus);
}
