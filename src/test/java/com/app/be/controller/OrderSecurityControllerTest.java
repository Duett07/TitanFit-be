package com.app.be.controller;

import com.app.be.config.SecurityConfig;
import com.app.be.controller.admin.AdminOrderController;
import com.app.be.dto.common.PageResponse;
import com.app.be.dto.order.CreateOrderRequest;
import com.app.be.dto.order.OrderItemRequest;
import com.app.be.dto.order.OrderResponse;
import com.app.be.dto.order.TrackOrderRequest;
import com.app.be.dto.order.UpdateOrderStatusRequest;
import com.app.be.enums.OrderStatus;
import com.app.be.enums.PaymentMethod;
import com.app.be.enums.PaymentStatus;
import com.app.be.enums.Role;
import com.app.be.exception.GlobalExceptionHandler;
import com.app.be.security.jwt.JwtAccessDeniedHandler;
import com.app.be.security.jwt.JwtAuthenticationEntryPoint;
import com.app.be.security.jwt.JwtAuthenticationFilter;
import com.app.be.security.user.CustomUserDetails;
import com.app.be.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        OrderSecurityControllerTest.TestWebConfig.class,
        SecurityConfig.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        AdminOrderController.class,
        OrderController.class,
        GlobalExceptionHandler.class
})
@WebAppConfiguration
class OrderSecurityControllerTest {

    @Configuration
    @EnableWebMvc
    static class TestWebConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws ServletException, IOException {
        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    private OrderResponse createSampleOrderResponse(UUID orderId) {
        return OrderResponse.builder()
                .id(orderId)
                .orderCode("TF-TEST1234")
                .customerName("Nguyen Van A")
                .phone("0912345678")
                .shippingAddress("123 Duong 3/2")
                .orderStatus(OrderStatus.PENDING.name())
                .paymentStatus(PaymentStatus.UNPAID.name())
                .paymentMethod(PaymentMethod.COD.name())
                .totalAmount(BigDecimal.valueOf(500000))
                .createdAt(LocalDateTime.now())
                .items(List.of())
                .build();
    }

    @Nested
    @DisplayName("Tests for Admin Endpoints Security (/api/admin/orders)")
    class AdminEndpointsSecurityTests {

        @Test
        @DisplayName("GET /api/admin/orders: Tra ve 401 khi chua dang nhap")
        void getAllOrders_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/api/admin/orders"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.message").value("Yêu cầu xác thực tài khoản để truy cập tài nguyên này"));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("GET /api/admin/orders: Tra ve 403 khi dang nhap voi vai tro CUSTOMER")
        void getAllOrders_CustomerRole_Returns403() throws Exception {
            mockMvc.perform(get("/api/admin/orders"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403))
                    .andExpect(jsonPath("$.message").value("Bạn không có quyền thực hiện hành động này"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /api/admin/orders: Tra ve 200 OK khi dang nhap voi vai tro ADMIN")
        void getAllOrders_AdminRole_Returns200() throws Exception {
            OrderResponse sample = createSampleOrderResponse(UUID.randomUUID());
            PageResponse<OrderResponse> pageResponse = PageResponse.<OrderResponse>builder()
                    .content(List.of(sample))
                    .pageNo(0)
                    .pageSize(10)
                    .totalElements(1)
                    .totalPages(1)
                    .isLast(true)
                    .build();

            when(orderService.getAllOrders(anyInt(), anyInt(), any(), any(), any(), any(), any()))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/api/admin/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.content[0].orderCode").value("TF-TEST1234"));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("PATCH /api/admin/orders/{id}/status: Tra ve 403 khi CUSTOMER co gang cap nhat")
        void updateOrderStatus_CustomerRole_Returns403() throws Exception {
            UUID orderId = UUID.randomUUID();
            UpdateOrderStatusRequest body = new UpdateOrderStatusRequest();
            body.setNewStatus(OrderStatus.CONFIRMED);

            mockMvc.perform(patch("/api/admin/orders/{id}/status", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("PATCH /api/admin/orders/{id}/status: Tra ve 200 OK khi ADMIN cap nhat trang thai")
        void updateOrderStatus_AdminRole_Returns200() throws Exception {
            UUID orderId = UUID.randomUUID();
            UpdateOrderStatusRequest body = new UpdateOrderStatusRequest();
            body.setNewStatus(OrderStatus.CONFIRMED);

            OrderResponse sample = createSampleOrderResponse(orderId);
            sample.setOrderStatus(OrderStatus.CONFIRMED.name());

            when(orderService.updateOrderStatus(eq(orderId), eq(OrderStatus.CONFIRMED)))
                    .thenReturn(sample);

            mockMvc.perform(patch("/api/admin/orders/{id}/status", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.orderStatus").value("CONFIRMED"));
        }
    }

    @Nested
    @DisplayName("Tests for Customer Endpoints Security (/api/orders)")
    class CustomerEndpointsSecurityTests {

        @Test
        @DisplayName("POST /api/orders/track: Endpoint cong khai, tra ve 200 OK khi khong co token")
        void trackOrder_PublicAccess_Returns200() throws Exception {
            TrackOrderRequest request = new TrackOrderRequest();
            request.setOrderCode("TF-TEST1234");
            request.setPhone("0912345678");

            OrderResponse sample = createSampleOrderResponse(UUID.randomUUID());
            when(orderService.trackOrder(any(TrackOrderRequest.class))).thenReturn(sample);

            mockMvc.perform(post("/api/orders/track")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.orderCode").value("TF-TEST1234"));
        }

        @Test
        @DisplayName("POST /api/orders: Bat buoc xac thuc, tra ve 401 khi chua dang nhap")
        void createOrder_Unauthenticated_Returns401() throws Exception {
            OrderItemRequest itemReq = new OrderItemRequest();
            itemReq.setVariantId(UUID.randomUUID());
            itemReq.setQuantity(1);

            CreateOrderRequest body = new CreateOrderRequest();
            body.setCustomerName("Nguyen Van A");
            body.setPhone("0912345678");
            body.setShippingAddress("123 Duong 3/2");
            body.setPaymentMethod(PaymentMethod.COD);
            body.setItems(List.of(itemReq));

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(401));
        }

        @Test
        @DisplayName("POST /api/orders: Cho phep dat hang khi da xac thuc voi tai khoan CUSTOMER")
        void createOrder_AuthenticatedCustomer_Returns201() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserDetails customerDetails = new CustomUserDetails(
                    userId,
                    "customer@titanfit.vn",
                    "password",
                    "Nguyen Van A",
                    List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")),
                    null
            );

            OrderItemRequest itemReq = new OrderItemRequest();
            itemReq.setVariantId(UUID.randomUUID());
            itemReq.setQuantity(1);

            CreateOrderRequest body = new CreateOrderRequest();
            body.setCustomerName("Nguyen Van A");
            body.setPhone("0912345678");
            body.setShippingAddress("123 Duong 3/2");
            body.setPaymentMethod(PaymentMethod.COD);
            body.setItems(List.of(itemReq));

            OrderResponse sample = createSampleOrderResponse(UUID.randomUUID());
            when(orderService.createOrder(any(CreateOrderRequest.class), eq(userId)))
                    .thenReturn(sample);

            mockMvc.perform(post("/api/orders")
                            .with(user(customerDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.orderCode").value("TF-TEST1234"));
        }

        @Test
        @DisplayName("GET /api/orders/my-orders: Tra ve 401 khi chua dang nhap")
        void getMyOrders_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/api/orders/my-orders"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(401));
        }

        @Test
        @DisplayName("GET /api/orders/my-orders: Tra ve 200 OK khi khach hang da dang nhap")
        void getMyOrders_AuthenticatedCustomer_Returns200() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserDetails customerDetails = new CustomUserDetails(
                    userId,
                    "customer@titanfit.vn",
                    "password",
                    "Nguyen Van A",
                    List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")),
                    null
            );

            PageResponse<OrderResponse> emptyPage = PageResponse.<OrderResponse>builder()
                    .content(List.of(createSampleOrderResponse(UUID.randomUUID())))
                    .pageNo(0)
                    .pageSize(10)
                    .totalElements(1)
                    .totalPages(1)
                    .isLast(true)
                    .build();

            when(orderService.getMyOrders(eq(userId), anyInt(), anyInt(), any()))
                    .thenReturn(emptyPage);

            mockMvc.perform(get("/api/orders/my-orders")
                            .with(user(customerDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.content[0].orderCode").value("TF-TEST1234"));
        }
    }
}
