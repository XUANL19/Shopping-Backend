package com.shopping.order.dao;

import com.shopping.order.entity.Order;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends CassandraRepository<Order, String> {
    Slice<Order> findByUserId(UUID userId, Pageable pageable);
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    long countByUserId(UUID userId);
}