package com.shopping.order.exception;

public class InvalidUserIdFormatException extends RuntimeException {
    public InvalidUserIdFormatException(String message) {
        super(message);
    }
}