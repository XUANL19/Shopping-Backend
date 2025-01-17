package com.shopping.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventDto {
    private String orderId;
    private UUID userId;
    private String orderStatus;
    private List<OrderItemDto> items;
}
