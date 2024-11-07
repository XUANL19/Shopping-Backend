package com.shopping.item.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ItemDto {
    @NotNull(message = "UPC is required")
    @Positive(message = "UPC must be positive")
    private Long upc;

    @NotBlank(message = "Item name is required")
    private String itemName;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private Double price;

    @NotNull(message = "Purchase limit is required")
    @Positive(message = "Purchase limit must be positive")
    private Integer purchaseLimit;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Inventory is required")
    @Positive(message = "Inventory must be positive")
    private Integer inventory;
}