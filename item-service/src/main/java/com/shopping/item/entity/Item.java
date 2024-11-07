package com.shopping.item.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

@Data
@Document(collection = "items")
public class Item {
    @Id
    private String id;

    @Indexed(unique = true)
    private Long upc;
    private String itemName;
    private Double price;
    private Integer purchaseLimit;
    private String category;
    private Integer inventory;
}