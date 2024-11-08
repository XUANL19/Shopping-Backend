package com.shopping.payment.kafka;

import com.shopping.payment.constants.PaymentConstants.OrderStatus;
import com.shopping.payment.dto.OrderEventDto;
import com.shopping.payment.exception.PaymentNotFoundException;
import com.shopping.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {
    private final PaymentService paymentService;

    @KafkaListener(
            topics = "order-status-updates",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderStatusUpdate(OrderEventDto orderEvent) {
        log.info("Received order status update: {}", orderEvent);
        try {
            switch (orderEvent.getOrderStatus()) {
                case OrderStatus.USER_CANCELED -> {
                    log.info("Processing order cancellation for order: {}", orderEvent.getOrderId());
                    paymentService.cancelPaymentByOrderStatusKafkaMessage(orderEvent.getOrderId().toString());
                }
                case OrderStatus.PAID -> {
                    log.info("Order is paid, no action needed in payment service: {}", orderEvent.getOrderId());
                }
                case OrderStatus.PAYMENT_FAILED -> {
                    log.info("Payment failed for order: {}", orderEvent.getOrderId());
                }
                case OrderStatus.CANCELLED -> {
                    log.info("Order was cancelled (not by user) for order: {}", orderEvent.getOrderId());
                }
                case OrderStatus.REPAY_NEEDED -> {
                    log.info("Repayment needed for order: {}", orderEvent.getOrderId());
                }
                default -> log.warn("Unhandled order status: {} for order: {}",
                        orderEvent.getOrderStatus(), orderEvent.getOrderId());
            }

        } catch (PaymentNotFoundException e) {
            log.warn("Payment not found for order status update: {}. Message will be discarded. ",
                    orderEvent.getOrderId());
        } catch (Exception e) {
            log.error("Error processing order status update for order {}: {}",
                    orderEvent.getOrderId(), e.getMessage());
            throw e; // Let the error handler deal with retries
        }
    }
}