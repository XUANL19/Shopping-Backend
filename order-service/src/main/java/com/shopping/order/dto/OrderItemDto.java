package com.shopping.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {
    @NotNull(message = "UPC is required")
    private Long upc;

    @Min(value = 1, message = "Purchase count must be at least 1")
    private Integer purchaseCount;
}