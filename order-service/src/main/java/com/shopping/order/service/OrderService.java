package com.shopping.order.service;

import com.shopping.order.constants.ErrorMessages;
import com.shopping.order.constants.OrderStatus;
import com.shopping.order.dao.OrderRepository;
import com.shopping.order.dto.*;
import com.shopping.common.dto.PageRequestDto;
import com.shopping.common.dto.PageResponseDto;
import com.shopping.order.entity.*;
import com.shopping.order.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PageResponseDto<OrderResponseDto> getOrdersByUserId(UUID userId, PageRequestDto pageRequest) {
        pageRequest.validate();

        Slice<Order> orderSlice = orderRepository.findByUserId(
                userId,
                PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize())
        );

        List<OrderResponseDto> orderDtos = orderSlice.getContent().stream()
                .map(this::mapToOrderResponseDto)
                .collect(Collectors.toList());

        PageResponseDto<OrderResponseDto> response = new PageResponseDto<>();
        response.setContent(orderDtos);
        response.setPageNumber(orderSlice.getNumber());
        response.setPageSize(orderSlice.getSize());
        response.setHasNext(orderSlice.hasNext());
        // manually calculate totalElements and totalPages
        long totalElements = orderRepository.countByUserId(userId);
        response.setTotalElements(totalElements);
        response.setTotalPages((int) Math.ceil((double) totalElements / pageRequest.getPageSize()));

        return response;
    }

    public OrderResponseDto getOrderById(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        String.format(ErrorMessages.ORDER_NOT_FOUND, orderId)));
        return mapToDto(order);
    }

    @Transactional
    public OrderResponseDto createOrder(CreateOrderRequestDto requestDto, String idempotencyKey) {
        try {
            // Check for duplicate order using idempotency key
            orderRepository.findByIdempotencyKey(idempotencyKey)
                    .ifPresent(order -> {
                        throw new DuplicateOrderException(
                                String.format("Order with idempotency key %s already exists", idempotencyKey)
                        );
                    });

            Order order = new Order();
            order.setOrderId(UUID.randomUUID().toString());
            order.setUserId(requestDto.getUserId());
            order.setOrderStatus("PENDING");
            order.setItems(mapToOrderItems(requestDto.getItems()));
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            order.setIdempotencyKey(idempotencyKey);
            Order savedOrder = orderRepository.save(order);

            try {
                kafkaTemplate.send("order-status-updates", mapToOrderEventDto(savedOrder))
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to send order event to Kafka: {}", ex.getMessage());
                            }
                        });
            } catch (Exception e) {
                log.error("Error sending message to Kafka: {}", e.getMessage());
            }

            return mapToOrderResponseDto(savedOrder);
        } catch (Exception e) {
            if (e instanceof DuplicateOrderException) {
                throw e;
            }
            throw new OrderCreationException("Error creating order: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void updateOrderStatus(PaymentStatusUpdateDto statusUpdate) {
        Order order = orderRepository.findById(statusUpdate.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(
                        String.format(ErrorMessages.ORDER_NOT_FOUND, statusUpdate.getOrderId())));

        String oldStatus = order.getOrderStatus();
        order.setOrderStatus(statusUpdate.getStatus());
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        // Only send to item service if status is PAID
        if ("PAID".equals(statusUpdate.getStatus())) {
            try {
                OrderEventDto orderEvent = mapToOrderEventDto(savedOrder);
                kafkaTemplate.send("order-status-updates", orderEvent)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to send order event to Kafka: {}", ex.getMessage());
                            } else {
                                log.info("Successfully sent inventory update for order: {}",
                                        order.getOrderId());
                            }
                        });
            } catch (Exception e) {
                log.error("Error sending message to Kafka for order {}: {}",
                        order.getOrderId(), e.getMessage(), e);
            }
        }
    }

    @Transactional
    public void deleteOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        String.format(ErrorMessages.ORDER_NOT_FOUND, orderId)));

        orderRepository.delete(order);
    }

    private List<OrderItem> mapToOrderItems(List<OrderItemDto> dtos) {
        return dtos.stream()
                .map(this::mapToOrderItem)
                .collect(Collectors.toList());
    }

    private OrderItem mapToOrderItem(OrderItemDto dto) {
        OrderItem item = new OrderItem();
        item.setUpc(dto.getUpc());
        item.setPurchaseCount(dto.getPurchaseCount());
        return item;
    }

    private OrderItemDto mapToOrderItemDto(OrderItem item) {
        OrderItemDto dto = new OrderItemDto();
        dto.setUpc(item.getUpc());
        dto.setPurchaseCount(item.getPurchaseCount());
        return dto;
    }

    private OrderEventDto mapToOrderEventDto(Order order) {
        return OrderEventDto.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .orderStatus(order.getOrderStatus())
                .items(order.getItems().stream()
                        .map(item -> OrderItemDto.builder()
                                .upc(item.getUpc())
                                .purchaseCount(item.getPurchaseCount())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private OrderResponseDto mapToOrderResponseDto(Order order) {
        return OrderResponseDto.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .orderStatus(order.getOrderStatus())
                .items(order.getItems().stream()
                        .map(this::mapToOrderItemDto)
                        .collect(Collectors.toList()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderResponseDto mapToDto(Order order) {
        OrderResponseDto dto = new OrderResponseDto();
        dto.setOrderId(order.getOrderId());
        dto.setUserId(order.getUserId());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setItems(order.getItems().stream()
                .map(this::mapToOrderItemDto)
                .collect(Collectors.toList()));
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        return dto;
    }

    private PageResponseDto<OrderResponseDto> createPageResponse(Page<Order> orderPage) {
        PageResponseDto<OrderResponseDto> response = new PageResponseDto<>();
        response.setContent(orderPage.getContent().stream()
                .map(this::mapToOrderResponseDto)
                .collect(Collectors.toList()));
        response.setPageNumber(orderPage.getNumber());
        response.setPageSize(orderPage.getSize());
        response.setTotalElements(orderPage.getTotalElements());
        response.setTotalPages(orderPage.getTotalPages());
        response.setHasNext(orderPage.hasNext());
        return response;
    }
}