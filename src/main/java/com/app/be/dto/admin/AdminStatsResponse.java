package com.app.be.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {

    private long totalOrders;
    private long pendingOrders;
    private long confirmedOrders;
    private long shippingOrders;
    private long completedOrders;
    private long cancelledOrders;
    private BigDecimal totalRevenue;
    private long totalProducts;
    private long activeProducts;
    private long totalCustomers;
}
