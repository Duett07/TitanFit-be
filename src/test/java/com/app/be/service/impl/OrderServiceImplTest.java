package com.app.be.service.impl;

import com.app.be.constant.OrderMessages;
import com.app.be.dto.order.CreateOrderRequest;
import com.app.be.dto.order.OrderItemRequest;
import com.app.be.dto.order.OrderResponse;
import com.app.be.dto.order.TrackOrderRequest;
import com.app.be.entity.Order;
import com.app.be.entity.OrderItem;
import com.app.be.entity.Product;
import com.app.be.entity.ProductVariant;
import com.app.be.entity.User;
import com.app.be.enums.OrderStatus;
import com.app.be.enums.PaymentMethod;
import com.app.be.enums.PaymentStatus;
import com.app.be.enums.Role;
import com.app.be.exception.BadRequestException;
import com.app.be.exception.ResourceNotFoundException;
import com.app.be.exception.UnauthorizedException;
import com.app.be.repository.OrderItemRepository;
import com.app.be.repository.OrderRepository;
import com.app.be.repository.ProductVariantRepository;
import com.app.be.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User sampleUser;
    private UUID sampleUserId;

    @BeforeEach
    void setUp() {
        sampleUserId = UUID.randomUUID();
        sampleUser = new User();
        sampleUser.setId(sampleUserId);
        sampleUser.setEmail("customer@titanfit.vn");
        sampleUser.setFullName("Nguyen Van A");
        sampleUser.setPhone("0912345678");
        sampleUser.setRole(Role.ROLE_CUSTOMER);
    }

    @Nested
    @DisplayName("Tests for createOrder()")
    class CreateOrderTests {

        private CreateOrderRequest createSampleRequest(UUID variantId, int quantity) {
            OrderItemRequest itemReq = new OrderItemRequest();
            itemReq.setVariantId(variantId);
            itemReq.setQuantity(quantity);

            CreateOrderRequest request = new CreateOrderRequest();
            request.setCustomerName("Nguyen Van A");
            request.setPhone("0912345678");
            request.setShippingAddress("123 Duong 3/2, Q10, TP.HCM");
            request.setNote("Giao gio hanh chinh");
            request.setPaymentMethod(PaymentMethod.COD);
            request.setItems(List.of(itemReq));
            return request;
        }

        @Test
        @DisplayName("createOrder: Thanh cong voi du lieu hop le, tru ton kho va tinh tong tien chinh xac")
        void createOrder_Success() {
            // Arrange
            UUID variantId = UUID.randomUUID();
            CreateOrderRequest request = createSampleRequest(variantId, 2);

            Product product = new Product();
            product.setId(UUID.randomUUID());
            product.setName("Titan Protein Whey");
            product.setActive(true);

            ProductVariant variant = new ProductVariant();
            variant.setId(variantId);
            variant.setProduct(product);
            variant.setVariantName("Hu 2kg Socola");
            variant.setPrice(BigDecimal.valueOf(500000));
            variant.setStockQuantity(10);
            variant.setSku("WHEY-CHO-2KG");

            when(userRepository.findById(sampleUserId)).thenReturn(Optional.of(sampleUser));
            when(orderRepository.existsByOrderCode(anyString())).thenReturn(false);
            when(productVariantRepository.findByIdForUpdate(variantId)).thenReturn(Optional.of(variant));
            when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> invocation.getArgument(0));

            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order orderToSave = invocation.getArgument(0);
                orderToSave.setId(UUID.randomUUID());
                orderToSave.setCreatedAt(LocalDateTime.now());
                return orderToSave;
            });

            when(orderItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            OrderResponse response = orderService.createOrder(request, sampleUserId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PENDING.name());
            assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID.name());
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000000));
            assertThat(response.getCustomerName()).isEqualTo("Nguyen Van A");

            // Verify variant stock deduction
            assertThat(variant.getStockQuantity()).isEqualTo(8);
            verify(productVariantRepository).save(variant);

            // Verify Order saved
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());
            Order capturedOrder = orderCaptor.getValue();
            assertThat(capturedOrder.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(capturedOrder.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
            assertThat(capturedOrder.getUser()).isEqualTo(sampleUser);
        }

        @Test
        @DisplayName("createOrder: Nem UnauthorizedException khi userId null")
        void createOrder_ThrowsUnauthorized_WhenUserIdNull() {
            CreateOrderRequest request = createSampleRequest(UUID.randomUUID(), 1);

            assertThatThrownBy(() -> orderService.createOrder(request, null))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage(OrderMessages.LOGIN_REQUIRED);
        }

        @Test
        @DisplayName("createOrder: Nem UnauthorizedException khi khong tim thay user")
        void createOrder_ThrowsUnauthorized_WhenUserNotFound() {
            CreateOrderRequest request = createSampleRequest(UUID.randomUUID(), 1);
            when(userRepository.findById(sampleUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.createOrder(request, sampleUserId))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage(OrderMessages.USER_NOT_FOUND_OR_LOCKED);
        }

        @Test
        @DisplayName("createOrder: Nem BadRequestException khi san pham dang bi an (isActive = false)")
        void createOrder_ThrowsBadRequest_WhenProductInactive() {
            UUID variantId = UUID.randomUUID();
            CreateOrderRequest request = createSampleRequest(variantId, 1);

            Product product = new Product();
            product.setName("San pham ngung ban");
            product.setActive(false);

            ProductVariant variant = new ProductVariant();
            variant.setId(variantId);
            variant.setProduct(product);
            variant.setVariantName("Bien the 1");
            variant.setPrice(BigDecimal.valueOf(100000));
            variant.setStockQuantity(10);

            when(userRepository.findById(sampleUserId)).thenReturn(Optional.of(sampleUser));
            when(orderRepository.existsByOrderCode(anyString())).thenReturn(false);
            when(productVariantRepository.findByIdForUpdate(variantId)).thenReturn(Optional.of(variant));

            assertThatThrownBy(() -> orderService.createOrder(request, sampleUserId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("ngừng kinh doanh");
        }

        @Test
        @DisplayName("createOrder: Nem BadRequestException khi ton kho khong du")
        void createOrder_ThrowsBadRequest_WhenInsufficientStock() {
            UUID variantId = UUID.randomUUID();
            CreateOrderRequest request = createSampleRequest(variantId, 10);

            Product product = new Product();
            product.setName("Ao tap TitanFit");
            product.setActive(true);

            ProductVariant variant = new ProductVariant();
            variant.setId(variantId);
            variant.setProduct(product);
            variant.setVariantName("Size L - Den");
            variant.setPrice(BigDecimal.valueOf(250000));
            variant.setStockQuantity(3); // Chi co 3 trong kho, khach dat 10

            when(userRepository.findById(sampleUserId)).thenReturn(Optional.of(sampleUser));
            when(orderRepository.existsByOrderCode(anyString())).thenReturn(false);
            when(productVariantRepository.findByIdForUpdate(variantId)).thenReturn(Optional.of(variant));

            assertThatThrownBy(() -> orderService.createOrder(request, sampleUserId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("không đủ hàng tồn kho");
        }

        @Test
        @DisplayName("createOrder: Nem BadRequestException khi gia san pham bang 0 hoac am")
        void createOrder_ThrowsBadRequest_WhenPriceInvalid() {
            UUID variantId = UUID.randomUUID();
            CreateOrderRequest request = createSampleRequest(variantId, 1);

            Product product = new Product();
            product.setName("Binh nuoc");
            product.setActive(true);

            ProductVariant variant = new ProductVariant();
            variant.setId(variantId);
            variant.setProduct(product);
            variant.setVariantName("750ml");
            variant.setPrice(BigDecimal.ZERO); // Gia 0 dong khong hop le
            variant.setStockQuantity(10);

            when(userRepository.findById(sampleUserId)).thenReturn(Optional.of(sampleUser));
            when(orderRepository.existsByOrderCode(anyString())).thenReturn(false);
            when(productVariantRepository.findByIdForUpdate(variantId)).thenReturn(Optional.of(variant));

            assertThatThrownBy(() -> orderService.createOrder(request, sampleUserId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("chưa có giá hợp lệ");
        }
    }

    @Nested
    @DisplayName("Tests for updateOrderStatus()")
    class UpdateOrderStatusTests {

        private Order createSampleOrder(UUID orderId, OrderStatus status, PaymentStatus paymentStatus, PaymentMethod method) {
            Order order = new Order();
            order.setId(orderId);
            order.setOrderCode("TF-TEST1234");
            order.setOrderStatus(status);
            order.setPaymentStatus(paymentStatus);
            order.setPaymentMethod(method);
            order.setCustomerName("Nguyen Van A");
            order.setPhone("0912345678");
            order.setShippingAddress("123 Duong 3/2");
            order.setTotalAmount(BigDecimal.valueOf(500000));
            order.setCreatedAt(LocalDateTime.now());
            return order;
        }

        @Test
        @DisplayName("updateOrderStatus: Chuyen tu PENDING sang CONFIRMED thanh cong")
        void updateOrderStatus_PendingToConfirmed_Success() {
            UUID orderId = UUID.randomUUID();
            Order order = createSampleOrder(orderId, OrderStatus.PENDING, PaymentStatus.UNPAID, PaymentMethod.COD);

            when(orderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            OrderResponse response = orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);

            assertThat(response).isNotNull();
            assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED.name());
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("updateOrderStatus: Huy don hang CANCELLED tu dong hoan kho va doi paymentStatus sang REFUNDED neu da PAID")
        void updateOrderStatus_CancelOrder_RestoresStockAndRefunds() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            Order order = createSampleOrder(orderId, OrderStatus.PENDING, PaymentStatus.PAID, PaymentMethod.VIETQR);

            UUID variantId = UUID.randomUUID();
            ProductVariant variant = new ProductVariant();
            variant.setId(variantId);
            variant.setVariantName("Protein 1kg");
            variant.setStockQuantity(5);
            variant.setSku("PROT-1KG");

            OrderItem item = new OrderItem();
            item.setId(UUID.randomUUID());
            item.setProductVariant(variant);
            item.setQuantity(3);
            item.setPrice(BigDecimal.valueOf(500000));
            item.setProductName("Protein Whey");
            item.setVariantName("1kg");

            order.setOrderItems(new ArrayList<>(List.of(item)));

            when(orderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
            when(productVariantRepository.findByIdForUpdate(variantId)).thenReturn(Optional.of(variant));
            when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            OrderResponse response = orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED);

            // Assert
            assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED.name());
            assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED.name());

            // Stock restored: 5 + 3 = 8
            assertThat(variant.getStockQuantity()).isEqualTo(8);
            verify(productVariantRepository).save(variant);
        }

        @Test
        @DisplayName("updateOrderStatus: Chuyen don COD sang COMPLETED tu dong doi paymentStatus sang PAID neu dang UNPAID")
        void updateOrderStatus_CompleteCodOrder_SetsPaid() {
            UUID orderId = UUID.randomUUID();
            Order order = createSampleOrder(orderId, OrderStatus.SHIPPING, PaymentStatus.UNPAID, PaymentMethod.COD);

            when(orderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            OrderResponse response = orderService.updateOrderStatus(orderId, OrderStatus.COMPLETED);

            assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED.name());
            assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PAID.name());
        }

        @Test
        @DisplayName("updateOrderStatus: Nem BadRequestException khi chuyen trang thai bat hop le (SHIPPING -> PENDING)")
        void updateOrderStatus_InvalidTransition_ThrowsBadRequest() {
            UUID orderId = UUID.randomUUID();
            Order order = createSampleOrder(orderId, OrderStatus.SHIPPING, PaymentStatus.PAID, PaymentMethod.COD);

            when(orderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, OrderStatus.PENDING))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Không thể chuyển trạng thái đơn hàng");
        }

        @Test
        @DisplayName("updateOrderStatus: Nem BadRequestException khi sua don da bi CANCELLED")
        void updateOrderStatus_CancelledOrder_ThrowsBadRequest() {
            UUID orderId = UUID.randomUUID();
            Order order = createSampleOrder(orderId, OrderStatus.CANCELLED, PaymentStatus.REFUNDED, PaymentMethod.COD);

            when(orderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage(OrderMessages.CANCELLED_CANNOT_CHANGE);
        }
    }

    @Nested
    @DisplayName("Tests for getOrderByCode()")
    class GetOrderByCodeTests {

        private Order createOrder(String orderCode, UUID ownerId) {
            Order order = new Order();
            order.setId(UUID.randomUUID());
            order.setOrderCode(orderCode);
            order.setOrderStatus(OrderStatus.PENDING);
            order.setPaymentStatus(PaymentStatus.UNPAID);
            order.setCustomerName("Nguyen Van A");
            order.setPhone("0912345678");
            order.setShippingAddress("123 Duong 3/2");
            order.setTotalAmount(BigDecimal.valueOf(300000));
            order.setCreatedAt(LocalDateTime.now());

            User owner = new User();
            owner.setId(ownerId);
            order.setUser(owner);
            return order;
        }

        @Test
        @DisplayName("getOrderByCode: Chu don hang lay thanh cong")
        void getOrderByCode_OwnerAccess_Success() {
            String code = "TF-ABCD1234";
            Order order = createOrder(code, sampleUserId);

            when(orderRepository.findWithDetailsByOrderCode(code)).thenReturn(Optional.of(order));

            OrderResponse response = orderService.getOrderByCode(code, sampleUserId, false);

            assertThat(response).isNotNull();
            assertThat(response.getOrderCode()).isEqualTo(code);
        }

        @Test
        @DisplayName("getOrderByCode: Admin xem duoc bat ky don hang nao du khong phai chu don")
        void getOrderByCode_AdminAccess_Success() {
            String code = "TF-ABCD1234";
            UUID differentUserId = UUID.randomUUID();
            Order order = createOrder(code, differentUserId);

            when(orderRepository.findWithDetailsByOrderCode(code)).thenReturn(Optional.of(order));

            OrderResponse response = orderService.getOrderByCode(code, sampleUserId, true);

            assertThat(response).isNotNull();
            assertThat(response.getOrderCode()).isEqualTo(code);
        }

        @Test
        @DisplayName("getOrderByCode: Nguoi dung khac khong phai Admin bi nem ResourceNotFoundException de bao mat")
        void getOrderByCode_NonOwnerNonAdmin_ThrowsNotFound() {
            String code = "TF-ABCD1234";
            UUID differentUserId = UUID.randomUUID();
            Order order = createOrder(code, differentUserId);

            when(orderRepository.findWithDetailsByOrderCode(code)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.getOrderByCode(code, sampleUserId, false))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(OrderMessages.ORDER_NOT_FOUND_PREFIX);
        }

        @Test
        @DisplayName("getOrderByCode: Chua dang nhap nem UnauthorizedException")
        void getOrderByCode_Unauthenticated_ThrowsUnauthorized() {
            assertThatThrownBy(() -> orderService.getOrderByCode("TF-ABCD1234", null, false))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage(OrderMessages.USER_UNAUTHENTICATED);
        }
    }

    @Nested
    @DisplayName("Tests for trackOrder()")
    class TrackOrderTests {

        @Test
        @DisplayName("trackOrder: Tra cuu dung ma don va so dien thoai (ho tro dinh dang +84)")
        void trackOrder_Success() {
            String code = "TF-TRACK123";
            Order order = new Order();
            order.setId(UUID.randomUUID());
            order.setOrderCode(code);
            order.setPhone("0912345678");
            order.setCustomerName("Nguyen Van A");
            order.setShippingAddress("Ha Noi");
            order.setOrderStatus(OrderStatus.SHIPPING);
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setTotalAmount(BigDecimal.valueOf(250000));
            order.setCreatedAt(LocalDateTime.now());

            when(orderRepository.findWithDetailsByOrderCode(code)).thenReturn(Optional.of(order));

            TrackOrderRequest request = new TrackOrderRequest();
            request.setOrderCode(code);
            request.setPhone("+84912345678"); // Format quoc te can duoc chuan hoa

            OrderResponse response = orderService.trackOrder(request);

            assertThat(response).isNotNull();
            assertThat(response.getOrderCode()).isEqualTo(code);
        }

        @Test
        @DisplayName("trackOrder: Sai so dien thoai nem ResourceNotFoundException")
        void trackOrder_PhoneMismatch_ThrowsNotFound() {
            String code = "TF-TRACK123";
            Order order = new Order();
            order.setOrderCode(code);
            order.setPhone("0912345678");

            when(orderRepository.findWithDetailsByOrderCode(code)).thenReturn(Optional.of(order));

            TrackOrderRequest request = new TrackOrderRequest();
            request.setOrderCode(code);
            request.setPhone("0987654321"); // Khong khop voi 0912345678

            assertThatThrownBy(() -> orderService.trackOrder(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(OrderMessages.ORDER_OR_PHONE_MISMATCH);
        }
    }
}
