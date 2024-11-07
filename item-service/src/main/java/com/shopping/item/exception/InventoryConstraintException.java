package com.shopping.item.exception;

public class InventoryConstraintException extends RuntimeException {
    public InventoryConstraintException(String message) {
        super(message);
    }
}