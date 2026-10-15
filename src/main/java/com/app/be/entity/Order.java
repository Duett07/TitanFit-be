package com.app.be.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.common.aliasing.qual.Unique;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new java.util.ArrayList<>();

    @Unique
    private String orderCode;
    private String customerName;
    private String phone;
    private String shippingAddress;
    private String note;
    private String paymentMethod;
    private String paymentStatus;
    private String orderStatus;
    private BigDecimal totalAmount;
}
