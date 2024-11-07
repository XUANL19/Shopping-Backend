package com.shopping.order.config;

import com.shopping.order.exception.OrderNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Slf4j
public class KafkaErrorHandlerConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (consumerRecord, exception) -> {
                    log.error("Error Handler - Failed to process message: " +
                                    "topic={}, key={}, timestamp={}, value={}, error={}",
                            consumerRecord.topic(),
                            consumerRecord.key(),
                            consumerRecord.timestamp(),
                            consumerRecord.value(),
                            exception.getMessage());

                    if (exception.getCause() != null) {
                        log.error("Root cause: ", exception.getCause());
                    }
                },
                // Retry 3 times, with 1 second interval
                new FixedBackOff(1000L, 3L)
        );

        // Don't retry for OrderNotFoundException
        errorHandler.addNotRetryableExceptions(OrderNotFoundException.class);

        return errorHandler;
    }
}