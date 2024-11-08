package com.shopping.payment.service;

import com.shopping.payment.constants.PaymentConstants;
import com.shopping.payment.dao.PaymentRepository;
import com.shopping.payment.dto.PaymentRequestDto;
import com.shopping.payment.dto.PaymentResponseDto;
import com.shopping.payment.dto.PaymentStatusUpdateDto;
import com.shopping.payment.dto.PaymentUpdateDto;
import com.shopping.payment.entity.Payment;
import com.shopping.payment.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final String TOPIC_PAYMENT_STATUS = "payment-status-updates";

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SecureRandom random = new SecureRandom();
    private final NavigableMap<Double, String> paymentStatusProbabilities = new TreeMap<>();

    // Initialize probability map
    {
        paymentStatusProbabilities.put(0.4, PaymentConstants.PaymentStatus.PAYMENT_SUCCESSFUL);  // 40% chance
        paymentStatusProbabilities.put(0.6, PaymentConstants.PaymentStatus.INSUFFICIENT_FUNDS);  // 20% chance
        paymentStatusProbabilities.put(0.8, PaymentConstants.PaymentStatus.FRAUDULENT_TRANSACTION);  // 20% chance
        paymentStatusProbabilities.put(1.0, PaymentConstants.PaymentStatus.CHARGEBACK_INITIATED);  // 20% chance
    }

    private String mockPaymentProcessing() {
        double randomValue = random.nextDouble(); // Returns a value between 0.0 and 1.0
        Map.Entry<Double, String> entry = paymentStatusProbabilities.higherEntry(randomValue);
        String selectedStatus = entry.getValue();
        log.debug("Random value: {}, Selected payment status: {}", randomValue, selectedStatus);
        return selectedStatus;
    }

    @Transactional
    public PaymentResponseDto createPayment(PaymentRequestDto requestDto, String idempotencyKey, UUID authenticatedUserId) {
        // Check for existing payment with the same order ID
        if (paymentRepository.existsByOrderId(requestDto.getOrderId())) {
            throw new DuplicatePaymentException(
                    String.format(PaymentConstants.ErrorMessages.DUPLICATE_ORDER_PAYMENT, requestDto.getOrderId()));
        }

        // Check for idempotency
        if (paymentRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new DuplicatePaymentException(
                    String.format(PaymentConstants.ErrorMessages.DUPLICATE_IDEMPOTENCY_KEY, idempotencyKey));
        }

        // Validate payment data
        validatePaymentData(requestDto);

        // Mock payment processing with random status
        String paymentStatus = mockPaymentProcessing();

        // Create payment record
        Payment payment = new Payment();
        payment.setOrderId(requestDto.getOrderId());
        payment.setUserId(authenticatedUserId);
        payment.setPaymentCard(requestDto.getPaymentCard());
        payment.setExpiration(requestDto.getExpiration());
        payment.setCvv(requestDto.getCvv());
        payment.setBillingAddress(requestDto.getBillingAddress());
        payment.setZip(requestDto.getZip());
        payment.setPaymentStatus(paymentStatus);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        // Send Kafka message
        sendPaymentStatusUpdatetoKafka(savedPayment);

        return mapToResponseDto(savedPayment);
    }

    @Transactional
    public PaymentResponseDto updatePayment(UUID paymentId, PaymentUpdateDto updateDto, UUID authenticatedUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        String.format(PaymentConstants.ErrorMessages.PAYMENT_NOT_FOUND, paymentId)));

        // Verify user authorization
        if (!payment.getUserId().equals(authenticatedUserId)) {
            throw new UnauthorizedPaymentAccessException(PaymentConstants.ErrorMessages.UNAUTHORIZED_PAYMENT_ACCESS);
        }

        // Check if payment can be updated
        if (PaymentConstants.PaymentStatus.PAYMENT_SUCCESSFUL.equals(payment.getPaymentStatus())) {
            throw new InvalidPaymentStatusException(PaymentConstants.ErrorMessages.SUCCESSFUL_PAYMENT_UPDATE_NOT_ALLOWED);
        }

        if (PaymentConstants.PaymentStatus.USER_CANCELED.equals(payment.getPaymentStatus())) {
            throw new InvalidPaymentStatusException(PaymentConstants.ErrorMessages.CANCELED_PAYMENT_UPDATE_NOT_ALLOWED);
        }

        // Update payment information
        updatePaymentFields(payment, updateDto);

        // Mock payment processing with random status
        String paymentStatus = mockPaymentProcessing();
        payment.setPaymentStatus(paymentStatus);
        payment.setUpdatedAt(LocalDateTime.now());

        Payment updatedPayment = paymentRepository.save(payment);

        // Send Kafka message
        sendPaymentStatusUpdatetoKafka(updatedPayment);

        return mapToResponseDto(updatedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentById(UUID paymentId, UUID authenticatedUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        String.format(PaymentConstants.ErrorMessages.PAYMENT_NOT_FOUND, paymentId)));

        if (!payment.getUserId().equals(authenticatedUserId)) {
            throw new UnauthorizedPaymentAccessException(PaymentConstants.ErrorMessages.UNAUTHORIZED_PAYMENT_ACCESS);
        }

        return mapToResponseDto(payment);
    }

    @Transactional
    public void cancelPaymentByOrderStatusKafkaMessage(String orderId) {
        Payment payment = paymentRepository.findByOrderId(UUID.fromString(orderId))
                .orElseThrow(() -> new PaymentNotFoundException(
                        String.format(PaymentConstants.ErrorMessages.ORDER_PAYMENT_NOT_FOUND, orderId)));

        // Check if payment can be canceled
        if (PaymentConstants.PaymentStatus.PAYMENT_SUCCESSFUL.equals(payment.getPaymentStatus())) {
            throw new InvalidPaymentStatusException(PaymentConstants.ErrorMessages.SUCCESSFUL_PAYMENT_CANCEL_NOT_ALLOWED);
        }

        payment.setPaymentStatus(PaymentConstants.PaymentStatus.USER_CANCELED);
        payment.setUpdatedAt(LocalDateTime.now());

        paymentRepository.save(payment);
        log.info("Payment status updated to USER_CANCELED for order: {}", orderId);
    }

    private void validatePaymentData(PaymentRequestDto requestDto) {
        // Validate card number
        if (!requestDto.getPaymentCard().matches("^[0-9]{16}$")) {
            throw new InvalidPaymentException(PaymentConstants.ErrorMessages.INVALID_PAYMENT_CARD);
        }

        // Validate expiration date
        if (!isValidExpirationDate(requestDto.getExpiration())) {
            throw new InvalidPaymentException(PaymentConstants.ErrorMessages.INVALID_EXPIRATION);
        }

        // Validate CVV
        if (!requestDto.getCvv().matches("^[0-9]{3,4}$")) {
            throw new InvalidPaymentException(PaymentConstants.ErrorMessages.INVALID_CVV);
        }

        // Validate ZIP
        if (!requestDto.getZip().matches("^[0-9]{5}$")) {
            throw new InvalidPaymentException(PaymentConstants.ErrorMessages.INVALID_ZIP);
        }
    }

    private boolean isValidExpirationDate(String expiration) {
        try {
            int month = Integer.parseInt(expiration.substring(0, 2));
            int year = Integer.parseInt(expiration.substring(2));

            if (month < 1 || month > 12) {
                return false;
            }

            // Convert to four-digit year
            int fullYear = 2000 + year;
            LocalDateTime expirationDate = LocalDateTime.of(fullYear, month, 1, 0, 0);

            return expirationDate.isAfter(LocalDateTime.now());
        } catch (Exception e) {
            return false;
        }
    }

    private void updatePaymentFields(Payment payment, PaymentUpdateDto updateDto) {
        if (updateDto.getPaymentCard() != null) {
            if (!updateDto.getPaymentCard().matches("^[0-9]{16}$")) {
                throw new InvalidPaymentException(PaymentConstants.ErrorMessages.INVALID_PAYMENT_CARD);
            }
            payment.setPaymentCard(updateDto.getPaymentCard());
        }

        if (updateDto.getExpiration() != null) {
            if (!isValidExpirationDate(updateDto.getExpiration())) {
                throw new InvalidPaymentException(PaymentConstants.ErrorMessages.INVALID_EXPIRATION);
            }
            payment.setExpiration(updateDto.getExpiration());
        }

        if (updateDto.getCvv() != null) {
            if (!updateDto.getCvv().matches("^[0-9]{3,4}$")) {
                throw new InvalidPaymentException(PaymentConstants.ErrorMessages.INVALID_CVV);
            }
            payment.setCvv(updateDto.getCvv());
        }

        if (updateDto.getBillingAddress() != null) {
            payment.setBillingAddress(updateDto.getBillingAddress());
        }

        if (updateDto.getZip() != null) {
            if (!updateDto.getZip().matches("^[0-9]{5}$")) {
                throw new InvalidPaymentException(PaymentConstants.ErrorMessages.INVALID_ZIP);
            }
            payment.setZip(updateDto.getZip());
        }
    }

    private void sendPaymentStatusUpdatetoKafka(Payment payment) {
        PaymentStatusUpdateDto statusUpdate = createStatusUpdate(payment);
        try {
            kafkaTemplate.send(TOPIC_PAYMENT_STATUS, payment.getOrderId().toString(), statusUpdate)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send payment status update: {}", ex.getMessage());
                        } else {
                            log.info("Payment status update sent for order: {}, status: {}, reason: {}",
                                    payment.getOrderId(),
                                    statusUpdate.getStatus(),
                                    statusUpdate.getReason());
                        }
                    });
        } catch (Exception e) {
            log.error("Error sending payment status update: {}", e.getMessage());
            throw new RuntimeException("Failed to send payment status update", e);
        }
    }

    private PaymentStatusUpdateDto createStatusUpdate(Payment payment) {
        String orderStatus = switch (payment.getPaymentStatus()) {
            case PaymentConstants.PaymentStatus.PAYMENT_SUCCESSFUL -> PaymentConstants.OrderStatus.PAID;
            case PaymentConstants.PaymentStatus.INSUFFICIENT_FUNDS -> PaymentConstants.OrderStatus.PAYMENT_FAILED;
            case PaymentConstants.PaymentStatus.FRAUDULENT_TRANSACTION -> PaymentConstants.OrderStatus.CANCELLED;
            case PaymentConstants.PaymentStatus.CHARGEBACK_INITIATED -> PaymentConstants.OrderStatus.REPAY_NEEDED;
            default -> throw new IllegalStateException("Unexpected payment status: " + payment.getPaymentStatus());
        };

        return PaymentStatusUpdateDto.builder()
                .orderId(payment.getOrderId())
                .status(orderStatus)
                .reason(payment.getPaymentStatus())
                .build();
    }

    private PaymentResponseDto mapToResponseDto(Payment payment) {
        return PaymentResponseDto.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .paymentCard("****" + payment.getPaymentCard().substring(payment.getPaymentCard().length() - 4))
                .paymentStatus(payment.getPaymentStatus())
                .build();
    }
}