package com.app.be.service.impl;

import com.app.be.dto.admin.AdminStatsResponse;
import com.app.be.enums.OrderStatus;
import com.app.be.enums.PaymentStatus;
import com.app.be.enums.Role;
import com.app.be.repository.OrderRepository;
import com.app.be.repository.ProductRepository;
import com.app.be.repository.UserRepository;
import com.app.be.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AdminStatsServiceImpl implements AdminStatsService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminStatsResponse getSystemStats() {
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByOrderStatus(OrderStatus.PENDING);
        long confirmedOrders = orderRepository.countByOrderStatus(OrderStatus.CONFIRMED);
        long shippingOrders = orderRepository.countByOrderStatus(OrderStatus.SHIPPING);
        long completedOrders = orderRepository.countByOrderStatus(OrderStatus.COMPLETED);
        long cancelledOrders = orderRepository.countByOrderStatus(OrderStatus.CANCELLED);

        // Doanh thu tính theo các đơn hàng đã thanh toán (PAID)
        BigDecimal totalRevenue = orderRepository.sumTotalAmountByPaymentStatus(PaymentStatus.PAID);
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        long totalProducts = productRepository.count();
        long activeProducts = productRepository.countByIsActiveTrue();
        long totalCustomers = userRepository.countByRole(Role.ROLE_CUSTOMER);

        return AdminStatsResponse.builder()
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .confirmedOrders(confirmedOrders)
                .shippingOrders(shippingOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .totalRevenue(totalRevenue)
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .totalCustomers(totalCustomers)
                .build();
    }
}
