package com.shopping.account.service;

import com.shopping.account.dao.UserRepository;
import com.shopping.account.dto.AuthResponseDto;
import com.shopping.account.dto.SignInRequestDto;
import com.shopping.account.dto.SignUpRequestDto;
import com.shopping.account.entity.User;
import com.shopping.account.exception.UserAlreadyExistsException;
import com.shopping.account.exception.InvalidCredentialsException;
import com.shopping.account.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthResponseDto signup(SignUpRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

        String token = jwtUtils.generateToken(user.getEmail());
        return new AuthResponseDto(token, user.getEmail());
    }

    public AuthResponseDto signin(SignInRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        String token = jwtUtils.generateToken(user.getEmail());
        return new AuthResponseDto(token, user.getEmail());
    }
}