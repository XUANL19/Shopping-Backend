package com.shopping.account.controller;

import com.shopping.account.dto.AuthResponseDto;
import com.shopping.account.dto.SignInRequestDto;
import com.shopping.account.dto.SignUpRequestDto;
import com.shopping.account.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponseDto> signup(@Valid @RequestBody SignUpRequestDto request) {
        return ResponseEntity.ok(userService.signup(request));
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponseDto> signin(@Valid @RequestBody SignInRequestDto request) {
        return ResponseEntity.ok(userService.signin(request));
    }
}