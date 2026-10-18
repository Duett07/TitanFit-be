package com.app.be.service;

import com.app.be.dto.order.CreateOrderRequest;
import com.app.be.dto.order.OrderItemRequest;
import com.app.be.dto.order.OrderResponse;
import com.app.be.dto.order.OrderStatus;
import com.app.be.dto.order.PaymentMethod;
import com.app.be.entity.Order;
import com.app.be.entity.OrderItem;
import com.app.be.entity.Product;
import com.app.be.entity.ProductVariant;
import com.app.be.exception.BadRequestException;
import com.app.be.exception.ResourceNotFoundException;
import com.app.be.repository.OrderItemRepository;
import com.app.be.repository.OrderRepository;
import com.app.be.repository.ProductVariantRepository;
import com.app.be.repository.UserRepository;
import com.app.be.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private UUID variantIdA;
    private UUID variantIdB;
    private ProductVariant variantA;
    private ProductVariant variantB;

    @BeforeEach
    void setUp() {
        variantIdA = UUID.fromString("00000000-0000-0000-0000-000000000001");
        variantIdB = UUID.fromString("00000000-0000-0000-0000-000000000002");

        Product product = new Product();
        product.setName("Whey Protein");
        product.setActive(true);

        variantA = new ProductVariant();
        variantA.setId(variantIdA);
        variantA.setSku("WHEY-VANILLA");
        variantA.setVariantName("Vanilla 2kg");
        variantA.setPrice(BigDecimal.valueOf(500_000));
        variantA.setStockQuantity(10);
        variantA.setProduct(product);

        variantB = new ProductVariant();
        variantB.setId(variantIdB);
        variantB.setSku("WHEY-CHOCO");
        variantB.setVariantName("Chocolate 2kg");
        variantB.setPrice(BigDecimal.valueOf(550_000));
        variantB.setStockQuantity(5);
        variantB.setProduct(product);
    }

    @Test
    @DisplayName("createOrder: Trừ kho thành công bằng Pessimistic Lock (findByIdForUpdate)")
    void shouldSuccessfullyDeductStockUsingPessimisticLock() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerName("Nguyen Van A")
                .phone("0987654321")
                .shippingAddress("123 Duong ABC, Ha Noi")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(
                        OrderItemRequest.builder().variantId(variantIdA).quantity(2).build()
                ))
                .build();

        when(productVariantRepository.findByIdForUpdate(variantIdA)).thenReturn(Optional.of(variantA));
        when(orderRepository.existsByOrderCode(any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response);
        assertEquals(8, variantA.getStockQuantity());
        verify(productVariantRepository).findByIdForUpdate(variantIdA);
        verify(productVariantRepository).save(variantA);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("createOrder: Ném BadRequestException khi tồn kho không đủ")
    void shouldThrowBadRequestExceptionWhenStockInsufficient() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerName("Nguyen Van A")
                .phone("0987654321")
                .shippingAddress("123 Duong ABC")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(
                        OrderItemRequest.builder().variantId(variantIdA).quantity(20).build()
                ))
                .build();

        when(productVariantRepository.findByIdForUpdate(variantIdA)).thenReturn(Optional.of(variantA));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> orderService.createOrder(request));
        assertTrue(ex.getMessage().contains("không đủ hàng tồn kho"));
        assertEquals(10, variantA.getStockQuantity());
        verify(productVariantRepository, never()).save(variantA);
    }

    @Test
    @DisplayName("createOrder: Ném BadRequestException khi sản phẩm đã bị ẩn (isActive = false)")
    void shouldThrowBadRequestWhenProductIsInactive() {
        variantA.getProduct().setActive(false);

        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerName("Nguyen Van A")
                .phone("0987654321")
                .shippingAddress("123 Duong ABC")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(
                        OrderItemRequest.builder().variantId(variantIdA).quantity(1).build()
                ))
                .build();

        when(productVariantRepository.findByIdForUpdate(variantIdA)).thenReturn(Optional.of(variantA));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> orderService.createOrder(request));
        assertTrue(ex.getMessage().contains("hiện không khả dụng hoặc đã ngừng kinh doanh"));
        verify(productVariantRepository, never()).save(variantA);
    }

    @Test
    @DisplayName("createOrder: Tự động gom nhóm duplicate variantId trong cùng request và trừ 1 lần")
    void shouldAggregateDuplicateVariantIdsInRequest() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerName("Nguyen Van A")
                .phone("0987654321")
                .shippingAddress("123 Duong ABC")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(
                        OrderItemRequest.builder().variantId(variantIdA).quantity(2).build(),
                        OrderItemRequest.builder().variantId(variantIdA).quantity(3).build()
                ))
                .build();

        when(productVariantRepository.findByIdForUpdate(variantIdA)).thenReturn(Optional.of(variantA));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response);
        assertEquals(5, variantA.getStockQuantity()); // 10 - (2 + 3) = 5
        verify(productVariantRepository, times(1)).findByIdForUpdate(variantIdA);
    }

    @Test
    @DisplayName("createOrder: Sắp xếp ID biến thể theo thứ tự nhất quán để chống Deadlock")
    void shouldSortVariantIdsToPreventDeadlock() {
        // Request gửi variant B trước, variant A sau (B > A)
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerName("Nguyen Van A")
                .phone("0987654321")
                .shippingAddress("123 Duong ABC")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(
                        OrderItemRequest.builder().variantId(variantIdB).quantity(1).build(),
                        OrderItemRequest.builder().variantId(variantIdA).quantity(1).build()
                ))
                .build();

        when(productVariantRepository.findByIdForUpdate(variantIdA)).thenReturn(Optional.of(variantA));
        when(productVariantRepository.findByIdForUpdate(variantIdB)).thenReturn(Optional.of(variantB));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        orderService.createOrder(request);

        // Kiểm tra thứ tự gọi findByIdForUpdate: A phải được gọi TRƯỚC B
        ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);
        verify(productVariantRepository, times(2)).findByIdForUpdate(captor.capture());

        List<UUID> capturedIds = captor.getAllValues();
        assertEquals(variantIdA, capturedIds.get(0));
        assertEquals(variantIdB, capturedIds.get(1));
    }

    @Test
    @DisplayName("updateOrderStatus: Hoàn trả tồn kho an toàn bằng findByIdForUpdate khi HỦY đơn")
    void shouldRestoreStockUsingPessimisticLockOnCancel() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setOrderStatus("PENDING");
        order.setOrderCode("TF-TEST123");

        OrderItem item = new OrderItem();
        item.setProductVariant(variantA);
        item.setQuantity(3);
        order.setOrderItems(List.of(item));

        when(orderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        when(productVariantRepository.findByIdForUpdate(variantIdA)).thenReturn(Optional.of(variantA));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED);

        assertNotNull(response);
        assertEquals("CANCELLED", response.getOrderStatus());
        assertEquals(13, variantA.getStockQuantity()); // 10 + 3 = 13
        verify(productVariantRepository).findByIdForUpdate(variantIdA);
        verify(productVariantRepository).save(variantA);
    }

    @Test
    @DisplayName("Kiểm thử đồng thời (Concurrency Test): 10 thread cùng đặt khi tồn kho = 5 -> chính xác 5 thành công, 5 thất bại, tồn kho = 0")
    void shouldHandleConcurrentOrdersAccuratelyUnderPessimisticLock() throws InterruptedException {
        int initialStock = 5;
        int totalThreads = 10;
        AtomicInteger currentStock = new AtomicInteger(initialStock);
        ReentrantLock dbRowLock = new ReentrantLock();

        // Giả lập hành vi khóa hàng của MySQL InnoDB:
        // findByIdForUpdate khóa hàng độc quyền cho đến khi transaction hoàn tất (sau khi save)
        when(productVariantRepository.findByIdForUpdate(variantIdA)).thenAnswer(inv -> {
            dbRowLock.lock();
            ProductVariant v = new ProductVariant();
            v.setId(variantIdA);
            v.setVariantName("Whey Concurrency");
            v.setPrice(BigDecimal.valueOf(100_000));
            v.setStockQuantity(currentStock.get());
            Product p = new Product();
            p.setName("Whey Protein");
            p.setActive(true);
            v.setProduct(p);
            return Optional.of(v);
        });

        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> {
            ProductVariant v = inv.getArgument(0);
            currentStock.set(v.getStockQuantity());
            return v;
        });

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch readyLatch = new CountDownLatch(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Đợi cả 10 thread sẵn sàng để bắn đồng thời
                    CreateOrderRequest req = CreateOrderRequest.builder()
                            .customerName("Customer Concurrent")
                            .phone("0987654321")
                            .shippingAddress("123 Concurrency St")
                            .paymentMethod(PaymentMethod.COD)
                            .items(List.of(
                                    OrderItemRequest.builder().variantId(variantIdA).quantity(1).build()
                            ))
                            .build();

                    try {
                        orderService.createOrder(req);
                        successCount.incrementAndGet();
                    } catch (BadRequestException ex) {
                        failureCount.incrementAndGet();
                    } finally {
                        if (dbRowLock.isHeldByCurrentThread()) {
                            dbRowLock.unlock(); // Commit / Rollback transaction
                        }
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown(); // Bắn 10 thread đồng thời
        doneLatch.await();
        executor.shutdown();

        // Kiểm tra kết quả: Chính xác 5 đơn thành công, 5 đơn thất bại vì hết hàng, tồn kho cuối = 0
        assertEquals(5, successCount.get(), "Chính xác 5 đơn hàng phải đặt thành công");
        assertEquals(5, failureCount.get(), "Chính xác 5 đơn hàng phải thất bại do hết tồn kho");
        assertEquals(0, currentStock.get(), "Tồn kho cuối cùng phải về đúng 0 (không bị âm hoặc bán vượt)");
    }
}
