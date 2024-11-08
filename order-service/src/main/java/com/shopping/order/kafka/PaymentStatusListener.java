package com.shopping.order.kafka;

import com.shopping.order.dto.PaymentStatusUpdateDto;
import com.shopping.order.service.OrderService;
import com.shopping.order.exception.OrderNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentStatusListener {
    private final OrderService orderService;

    @KafkaListener(
            topics = "payment-status-updates",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "paymentKafkaListenerContainerFactory"
    )
    public void handlePaymentStatusUpdate(PaymentStatusUpdateDto statusUpdate) {
        try {
            log.info("Received payment status update: {}", statusUpdate);
            orderService.updateOrderStatusByPaymentStatusKafkaMessage(statusUpdate);
            log.info("Successfully processed payment status update for order: {}",
                    statusUpdate.getOrderId());
        } catch (OrderNotFoundException ex) {
            log.warn("Order not found for payment status update: {}. Message will be discarded. " +
                    "Reason: {}", statusUpdate.getOrderId(), statusUpdate.getReason());
        } catch (Exception e) {
            log.error("Error processing payment status update for order {}. " +
                            "Status: {}, Reason: {}, Error: {}",
                    statusUpdate.getOrderId(),
                    statusUpdate.getStatus(),
                    statusUpdate.getReason(),
                    e.getMessage());
            throw e;
        }
    }
}