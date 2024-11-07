package com.shopping.item.exception;

public class DuplicateUpcException extends RuntimeException {
    public DuplicateUpcException(String message) {
        super(message);
    }
}