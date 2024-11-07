package com.shopping.order.entity;

import lombok.Data;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@Data
@UserDefinedType("order_item")
public class OrderItem {
    private Long upc;

    @Column("purchase_count")
    private Integer purchaseCount;
}