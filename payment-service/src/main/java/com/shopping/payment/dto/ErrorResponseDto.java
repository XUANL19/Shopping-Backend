package com.shopping.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ErrorResponseDto {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    public static ErrorResponseDto of(int status, String error, String message, String path) {
        return new ErrorResponseDto(LocalDateTime.now(), status, error, message, path);
    }
}