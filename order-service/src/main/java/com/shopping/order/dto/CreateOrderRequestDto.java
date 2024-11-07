package com.shopping.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
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
public class CreateOrderRequestDto {
    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotEmpty(message = "Order must contain at least one item")
    private List<OrderItemDto> items;
}