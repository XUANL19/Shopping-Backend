package com.shopping.payment.constants;

public class PaymentConstants {
    public static class PaymentStatus {
        public static final String PAYMENT_SUCCESSFUL = "Payment Successful";
        public static final String INSUFFICIENT_FUNDS = "Insufficient Funds";
        public static final String FRAUDULENT_TRANSACTION = "Fraudulent Transaction";
        public static final String CHARGEBACK_INITIATED = "Chargeback Initiated";
    }

    public static class OrderStatus {
        public static final String PAID = "PAID";
        public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
        public static final String CANCELLED = "CANCELLED";
        public static final String REPAY_NEEDED = "REPAY_NEEDED";
    }

    public static class ErrorMessages {
        public static final String PAYMENT_NOT_FOUND = "Payment not found with ID: %s";
        public static final String DUPLICATE_ORDER_PAYMENT = "Payment already exists for Order ID: %s";
        public static final String DUPLICATE_IDEMPOTENCY_KEY = "Duplicate idempotency key: %s";
        public static final String UNAUTHORIZED_PAYMENT_ACCESS = "User not authorized to access this payment";
        public static final String INVALID_PAYMENT_CARD = "Invalid payment card number";
        public static final String INVALID_EXPIRATION = "Invalid expiration date";
        public static final String INVALID_CVV = "Invalid CVV";
        public static final String INVALID_ZIP = "Invalid ZIP code";
        public static final String SUCCESSFUL_PAYMENT_UPDATE_NOT_ALLOWED =
                "Cannot update payment details after successful payment";
    }
}