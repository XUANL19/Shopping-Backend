package com.shopping.item.dao;

import com.shopping.item.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface ItemRepository extends MongoRepository<Item, String> {
    Optional<Item> findByUpc(Long upc);
    Page<Item> findAll(Pageable pageable);
    Page<Item> findByItemNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Item> findByCategory(String category, Pageable pageable);
    boolean existsByUpc(Long upc);
}