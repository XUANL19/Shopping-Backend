package com.shopping.order.service;

import com.shopping.common.dto.PageRequestDto;
import com.shopping.common.dto.PageResponseDto;
import com.shopping.order.constants.OrderStatus;
import com.shopping.order.dao.OrderRepository;
import com.shopping.order.dto.*;
import com.shopping.order.entity.Order;
import com.shopping.order.entity.OrderItem;
import com.shopping.order.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private OrderItem testOrderItem;
    private CreateOrderRequestDto createOrderRequest;
    private OrderUpdateDto updateOrderRequest;
    private static final String TEST_ORDER_ID = UUID.randomUUID().toString();
    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final Long TEST_UPC = 1234567890L;

    @BeforeEach
    void setUp() {
        // Setup test OrderItem
        testOrderItem = new OrderItem();
        testOrderItem.setUpc(TEST_UPC);
        testOrderItem.setPurchaseCount(2);

        // Setup test Order
        testOrder = new Order();
        testOrder.setOrderId(TEST_ORDER_ID);
        testOrder.setUserId(TEST_USER_ID);
        testOrder.setOrderStatus(OrderStatus.PENDING);
        testOrder.setItems(Collections.singletonList(testOrderItem));
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setUpdatedAt(LocalDateTime.now());
        testOrder.setIdempotencyKey("test-key");

        // Setup CreateOrderRequestDto
        OrderItemDto orderItemDto = OrderItemDto.builder()
                .upc(TEST_UPC)
                .purchaseCount(2)
                .build();
        createOrderRequest = CreateOrderRequestDto.builder()
                .userId(TEST_USER_ID)
                .items(Collections.singletonList(orderItemDto))
                .build();

        // Setup OrderUpdateDto
        updateOrderRequest = new OrderUpdateDto();
        updateOrderRequest.setItems(Collections.singletonList(orderItemDto));
    }

    @Test
    void createOrder_WhenNewOrder_ShouldCreateSuccessfully() {
        // Arrange
        String idempotencyKey = "test-key";
        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(kafkaTemplate.send(anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        // Act
        OrderResponseDto result = orderService.createOrder(createOrderRequest, idempotencyKey);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(OrderStatus.PENDING, result.getOrderStatus());
        verify(orderRepository).save(any(Order.class));
        verify(kafkaTemplate).send(eq("order-status-updates"), any(OrderEventDto.class));
    }

    @Test
    void createOrder_WhenDuplicateIdempotencyKey_ShouldThrowException() {
        // Arrange
        String idempotencyKey = "test-key";
        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThrows(DuplicateOrderException.class,
                () -> orderService.createOrder(createOrderRequest, idempotencyKey));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getOrdersByUserId_ShouldReturnPageResponse() {
        // Arrange
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageNumber(0);
        pageRequest.setPageSize(10);

        List<Order> orders = Collections.singletonList(testOrder);
        Slice<Order> orderSlice = new SliceImpl<>(orders);

        when(orderRepository.findByUserId(eq(TEST_USER_ID), any(PageRequest.class)))
                .thenReturn(orderSlice);
        when(orderRepository.countByUserId(TEST_USER_ID)).thenReturn(1L);

        // Act
        PageResponseDto<OrderResponseDto> result = orderService.getOrdersByUserId(TEST_USER_ID, pageRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(TEST_ORDER_ID, result.getContent().get(0).getOrderId());
    }

    @Test
    void updateOrderItems_WhenOrderIsPaid_ShouldThrowException() {
        // Arrange
        testOrder.setOrderStatus(OrderStatus.PAID);
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThrows(InvalidOrderStatusException.class,
                () -> orderService.updateOrderItems(TEST_ORDER_ID, updateOrderRequest));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrderItems_WhenOrderIsUserCanceled_ShouldThrowException() {
        // Arrange
        testOrder.setOrderStatus(OrderStatus.USER_CANCELED);
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThrows(InvalidOrderStatusException.class,
                () -> orderService.updateOrderItems(TEST_ORDER_ID, updateOrderRequest));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancelOrder_WhenOrderIsPaid_ShouldThrowException() {
        // Arrange
        testOrder.setOrderStatus(OrderStatus.PAID);
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThrows(InvalidOrderStatusException.class,
                () -> orderService.cancelOrder(TEST_ORDER_ID));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancelOrder_WhenOrderIsCancelable_ShouldCancel() {
        // Arrange
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(kafkaTemplate.send(anyString(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));

        // Act
        OrderResponseDto result = orderService.cancelOrder(TEST_ORDER_ID);

        // Assert
        assertEquals(OrderStatus.USER_CANCELED, result.getOrderStatus());
        verify(orderRepository).save(any(Order.class));
        verify(kafkaTemplate).send(eq("order-status-updates"), any(), any(OrderEventDto.class));
    }

    @Test
    void updateOrderStatusByPaymentStatusKafkaMessage_WhenStatusIsPaid_ShouldUpdateAndSendKafka() {
        // Arrange
        PaymentStatusUpdateDto statusUpdate = new PaymentStatusUpdateDto();
        statusUpdate.setOrderId(TEST_ORDER_ID);
        statusUpdate.setStatus("PAID");

        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(kafkaTemplate.send(anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        // Act
        orderService.updateOrderStatusByPaymentStatusKafkaMessage(statusUpdate);

        // Assert
        verify(orderRepository).save(any(Order.class));
        verify(kafkaTemplate).send(eq("order-status-updates"), any(OrderEventDto.class));
    }

    @Test
    void deleteOrder_WhenOrderExists_ShouldDelete() {
        // Arrange
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.of(testOrder));

        // Act
        orderService.deleteOrder(TEST_ORDER_ID);

        // Assert
        verify(orderRepository).delete(testOrder);
    }

    @Test
    void deleteOrder_WhenOrderDoesNotExist_ShouldThrowException() {
        // Arrange
        when(orderRepository.findById(TEST_ORDER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(OrderNotFoundException.class,
                () -> orderService.deleteOrder(TEST_ORDER_ID));
        verify(orderRepository, never()).delete(any(Order.class));
    }
}