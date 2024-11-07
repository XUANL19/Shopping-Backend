package com.shopping.order.exception;

public class OrderCreationException extends RuntimeException {
    public OrderCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}