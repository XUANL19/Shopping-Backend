package com.shopping.item.kafka;

import com.shopping.item.dto.OrderEventDto;
import com.shopping.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {
    private final ItemService itemService;

    @KafkaListener(
            topics = "order-status-updates",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderStatusUpdate(OrderEventDto orderEvent) {
        log.info("Received order status update: {}", orderEvent);
        try {
            if ("PAID".equals(orderEvent.getOrderStatus())) {
                itemService.processOrderPaid(orderEvent);
                log.info("Successfully processed paid order: {}", orderEvent.getOrderId());
            }
        } catch (Exception e) {
            log.error("Error processing order: {}", e.getMessage());
            throw e;
        }
    }
}