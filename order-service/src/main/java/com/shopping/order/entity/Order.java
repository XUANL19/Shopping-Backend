package com.shopping.order.entity;

import lombok.Data;
import org.springframework.data.cassandra.core.mapping.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Table("orders")
public class Order {
    @PrimaryKey("order_id")
    private String orderId;

    @Column("user_id")
    private UUID userId;

    @Column("order_status")
    private String orderStatus;

    @Column("items")
    private List<OrderItem> items;

    @Column("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime createdAt;

    @Column("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime updatedAt;

    @Column("idempotency_key")
    @Indexed
    private String idempotencyKey;
}