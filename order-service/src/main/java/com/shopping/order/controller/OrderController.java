package com.shopping.order.controller;

import com.shopping.order.dto.*;
import com.shopping.common.dto.PageRequestDto;
import com.shopping.common.dto.PageResponseDto;
import com.shopping.common.dto.ApiResponseDto;
import com.shopping.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponseDto<OrderResponseDto>> getOrderById(@PathVariable String orderId) {
        OrderResponseDto order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponseDto.<OrderResponseDto>builder()
                .status(HttpStatus.OK.value())
                .message("Order retrieved successfully")
                .data(order)
                .build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponseDto<PageResponseDto<OrderResponseDto>>> getOrdersByUserId(
            @PathVariable UUID userId,
            @RequestParam(required = false, defaultValue = "0") Integer pageNumber,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {

        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageNumber(pageNumber);
        pageRequest.setPageSize(pageSize);
        pageRequest.validate();

        PageResponseDto<OrderResponseDto> orders = orderService.getOrdersByUserId(userId, pageRequest);

        return ResponseEntity.ok(ApiResponseDto.<PageResponseDto<OrderResponseDto>>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Orders retrieved successfully")
                .data(orders)
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponseDto<OrderResponseDto>> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequestDto request) {
        OrderResponseDto order = orderService.createOrder(request, idempotencyKey);
        return ResponseEntity.status(201).body(ApiResponseDto.<OrderResponseDto>builder()
                .timestamp(LocalDateTime.now())
                .status(201)
                .message("Order created successfully")
                .data(order)
                .build());
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteOrder(@PathVariable String orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.ok(ApiResponseDto.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Order deleted successfully")
                .build());
    }
}