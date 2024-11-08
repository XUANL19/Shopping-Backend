package com.shopping.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {
    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotNull(message = "Payment card number is required")
    @Pattern(regexp = "^[0-9]{16}$", message = "Invalid card number format")
    private String paymentCard;

    @NotNull(message = "Expiration date is required")
    @Pattern(regexp = "^(0[1-9]|1[0-2])(2[4-9]|[3-9][0-9])$", message = "Invalid expiration date format (MMYY)")
    private String expiration;

    @NotNull(message = "CVV is required")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "Invalid CVV format")
    private String cvv;

    @NotNull(message = "Billing address is required")
    @Size(min = 5, message = "Billing address is too short")
    private String billingAddress;

    @NotNull(message = "ZIP code is required")
    @Pattern(regexp = "^[0-9]{5}$", message = "Invalid ZIP code format")
    private String zip;
}