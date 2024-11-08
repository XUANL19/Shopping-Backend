package com.shopping.account.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.UUID;

@Data
@AllArgsConstructor
public class AuthResponseDto {
    private String token;
    private String email;
    private UUID userId;
}