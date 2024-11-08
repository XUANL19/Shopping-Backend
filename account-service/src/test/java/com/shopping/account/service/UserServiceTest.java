package com.shopping.account.service;

import com.shopping.account.dao.UserRepository;
import com.shopping.account.dto.AuthResponseDto;
import com.shopping.account.dto.SignInRequestDto;
import com.shopping.account.dto.SignUpRequestDto;
import com.shopping.account.entity.User;
import com.shopping.account.exception.InvalidCredentialsException;
import com.shopping.account.exception.UserAlreadyExistsException;
import com.shopping.account.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private UserService userService;

    private SignUpRequestDto signUpRequest;
    private SignInRequestDto signInRequest;
    private User user;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_ENCODED_PASSWORD = "encodedPassword123";
    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final String TEST_TOKEN = "testToken123";

    @BeforeEach
    void setUp() {
        // Setup SignUpRequestDto
        signUpRequest = new SignUpRequestDto();
        signUpRequest.setEmail(TEST_EMAIL);
        signUpRequest.setPassword(TEST_PASSWORD);

        // Setup SignInRequestDto
        signInRequest = new SignInRequestDto();
        signInRequest.setEmail(TEST_EMAIL);
        signInRequest.setPassword(TEST_PASSWORD);

        // Setup User
        user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setPassword(TEST_ENCODED_PASSWORD);
    }

    @Test
    void signup_WhenEmailNotExists_ShouldCreateNewUser() {
        // Arrange
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(TEST_ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtUtils.generateToken(TEST_EMAIL, TEST_USER_ID)).thenReturn(TEST_TOKEN);

        // Act
        AuthResponseDto response = userService.signup(signUpRequest);

        // Assert
        assertNotNull(response);
        assertEquals(TEST_TOKEN, response.getToken());
        assertEquals(TEST_EMAIL, response.getEmail());
        assertEquals(TEST_USER_ID, response.getUserId());
        verify(userRepository).existsByEmail(TEST_EMAIL);
        verify(passwordEncoder).encode(TEST_PASSWORD);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void signup_WhenEmailExists_ShouldThrowException() {
        // Arrange
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> userService.signup(signUpRequest));
        verify(userRepository).existsByEmail(TEST_EMAIL);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void signin_WhenValidCredentials_ShouldReturnAuthResponse() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(TEST_PASSWORD, TEST_ENCODED_PASSWORD)).thenReturn(true);
        when(jwtUtils.generateToken(TEST_EMAIL, TEST_USER_ID)).thenReturn(TEST_TOKEN);

        // Act
        AuthResponseDto response = userService.signin(signInRequest);

        // Assert
        assertNotNull(response);
        assertEquals(TEST_TOKEN, response.getToken());
        assertEquals(TEST_EMAIL, response.getEmail());
        assertEquals(TEST_USER_ID, response.getUserId());
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordEncoder).matches(TEST_PASSWORD, TEST_ENCODED_PASSWORD);
    }

    @Test
    void signin_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> userService.signin(signInRequest));
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void signin_WhenInvalidPassword_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(TEST_PASSWORD, TEST_ENCODED_PASSWORD)).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> userService.signin(signInRequest));
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordEncoder).matches(TEST_PASSWORD, TEST_ENCODED_PASSWORD);
    }
}