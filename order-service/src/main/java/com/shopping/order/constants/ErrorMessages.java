package com.shopping.order.constants;

public class ErrorMessages {
    public static final String ORDER_NOT_FOUND = "Order not found with ID: %s";
    public static final String DUPLICATE_ORDER = "Order with idempotency key %s already exists";
    public static final String INVALID_ORDER_STATUS = "Invalid order status: %s";
    public static final String EMPTY_ORDER = "Order must contain at least one item";
    public static final String USER_NOT_FOUND = "User not found: %s";
}