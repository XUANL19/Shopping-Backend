package com.shopping.payment.controller;

import com.shopping.common.dto.ApiResponseDto;
import com.shopping.common.security.SecurityUtils;
import com.shopping.payment.dto.*;
import com.shopping.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<PaymentResponseDto>> createPayment(
            @Valid @RequestBody PaymentRequestDto paymentRequest,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        PaymentResponseDto response = paymentService.createPayment(paymentRequest, idempotencyKey, currentUserId);
        return new ResponseEntity<>(
                ApiResponseDto.created("Payment info created", response),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{paymentId}")
    public ResponseEntity<ApiResponseDto<PaymentResponseDto>> updatePayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody PaymentUpdateDto updateRequest) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        PaymentResponseDto response = paymentService.updatePayment(paymentId, updateRequest, currentUserId);
        return ResponseEntity.ok(ApiResponseDto.success("Payment info updated", response));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponseDto<PaymentResponseDto>> getPayment(
            @PathVariable UUID paymentId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        PaymentResponseDto payment = paymentService.getPaymentById(paymentId, currentUserId);
        return ResponseEntity.ok(ApiResponseDto.success("Successfully get Payment info", payment));
    }
}