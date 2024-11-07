package com.shopping.item.dto;

import lombok.Data;

@Data
public class ItemUpdateDto {
    private String itemName;
    private Double price;
    private Integer purchaseLimit;
    private String category;
    private Integer inventory;
}