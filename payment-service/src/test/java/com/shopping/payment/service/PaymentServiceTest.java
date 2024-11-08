package com.shopping.payment.service;

import com.shopping.payment.constants.PaymentConstants;
import com.shopping.payment.dao.PaymentRepository;
import com.shopping.payment.dto.PaymentRequestDto;
import com.shopping.payment.dto.PaymentResponseDto;
import com.shopping.payment.dto.PaymentStatusUpdateDto;
import com.shopping.payment.dto.PaymentUpdateDto;
import com.shopping.payment.entity.Payment;
import com.shopping.payment.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentService paymentService;

    @Captor
    private ArgumentCaptor<Payment> paymentCaptor;

    private Payment testPayment;
    private PaymentRequestDto validPaymentRequest;
    private PaymentUpdateDto validUpdateRequest;
    private static final UUID TEST_PAYMENT_ID = UUID.randomUUID();
    private static final UUID TEST_ORDER_ID = UUID.randomUUID();
    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final String VALID_CARD = "4111111111111111";
    private static final String VALID_EXPIRATION = "1225";
    private static final String VALID_CVV = "123";
    private static final String VALID_ADDRESS = "123 Test St";
    private static final String VALID_ZIP = "12345";

    @BeforeEach
    void setUp() {
        // Setup test Payment
        testPayment = new Payment();
        testPayment.setPaymentId(TEST_PAYMENT_ID);
        testPayment.setOrderId(TEST_ORDER_ID);
        testPayment.setUserId(TEST_USER_ID);
        testPayment.setPaymentCard(VALID_CARD);
        testPayment.setExpiration(VALID_EXPIRATION);
        testPayment.setCvv(VALID_CVV);
        testPayment.setBillingAddress(VALID_ADDRESS);
        testPayment.setZip(VALID_ZIP);
        testPayment.setPaymentStatus(PaymentConstants.PaymentStatus.PAYMENT_SUCCESSFUL);
        testPayment.setCreatedAt(LocalDateTime.now());
        testPayment.setUpdatedAt(LocalDateTime.now());

        // Setup valid payment request
        validPaymentRequest = PaymentRequestDto.builder()
                .orderId(TEST_ORDER_ID)
                .paymentCard(VALID_CARD)
                .expiration(VALID_EXPIRATION)
                .cvv(VALID_CVV)
                .billingAddress(VALID_ADDRESS)
                .zip(VALID_ZIP)
                .build();

        // Setup valid update request
        validUpdateRequest = PaymentUpdateDto.builder()
                .paymentCard(VALID_CARD)
                .expiration(VALID_EXPIRATION)
                .cvv(VALID_CVV)
                .billingAddress(VALID_ADDRESS)
                .zip(VALID_ZIP)
                .build();
    }

    @Test
    void createPayment_WhenValidRequest_ShouldCreateSuccessfully() {
        // Arrange
        String idempotencyKey = "test-key";
        when(paymentRepository.existsByOrderId(TEST_ORDER_ID)).thenReturn(false);
        when(paymentRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        PaymentResponseDto result = paymentService.createPayment(validPaymentRequest, idempotencyKey, TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_PAYMENT_ID, result.getPaymentId());
        assertTrue(result.getPaymentCard().startsWith("****"));
        verify(paymentRepository).save(paymentCaptor.capture());
        verify(kafkaTemplate).send(anyString(), anyString(), any(PaymentStatusUpdateDto.class));

        Payment savedPayment = paymentCaptor.getValue();
        assertEquals(TEST_USER_ID, savedPayment.getUserId());
        assertEquals(idempotencyKey, savedPayment.getIdempotencyKey());
    }

    @Test
    void createPayment_WhenDuplicateOrderId_ShouldThrowException() {
        // Arrange
        when(paymentRepository.existsByOrderId(TEST_ORDER_ID)).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicatePaymentException.class,
                () -> paymentService.createPayment(validPaymentRequest, "test-key", TEST_USER_ID));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void updatePayment_WhenValidRequest_ShouldUpdate() {
        // Arrange
        testPayment.setPaymentStatus(PaymentConstants.PaymentStatus.INSUFFICIENT_FUNDS);
        when(paymentRepository.findById(TEST_PAYMENT_ID)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        PaymentResponseDto result = paymentService.updatePayment(TEST_PAYMENT_ID, validUpdateRequest, TEST_USER_ID);

        // Assert
        assertNotNull(result);
        verify(paymentRepository).save(any(Payment.class));
        verify(kafkaTemplate).send(anyString(), anyString(), any(PaymentStatusUpdateDto.class));
    }

    @Test
    void updatePayment_WhenPaymentSuccessful_ShouldThrowException() {
        // Arrange
        testPayment.setPaymentStatus(PaymentConstants.PaymentStatus.PAYMENT_SUCCESSFUL);
        when(paymentRepository.findById(TEST_PAYMENT_ID)).thenReturn(Optional.of(testPayment));

        // Act & Assert
        assertThrows(InvalidPaymentStatusException.class,
                () -> paymentService.updatePayment(TEST_PAYMENT_ID, validUpdateRequest, TEST_USER_ID));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void getPaymentById_WhenPaymentExists_ShouldReturnPayment() {
        // Arrange
        when(paymentRepository.findById(TEST_PAYMENT_ID)).thenReturn(Optional.of(testPayment));

        // Act
        PaymentResponseDto result = paymentService.getPaymentById(TEST_PAYMENT_ID, TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_PAYMENT_ID, result.getPaymentId());
        assertTrue(result.getPaymentCard().startsWith("****"));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void getPaymentById_WhenUnauthorizedUser_ShouldThrowException() {
        // Arrange
        UUID differentUserId = UUID.randomUUID();
        when(paymentRepository.findById(TEST_PAYMENT_ID)).thenReturn(Optional.of(testPayment));

        // Act & Assert
        assertThrows(UnauthorizedPaymentAccessException.class,
                () -> paymentService.getPaymentById(TEST_PAYMENT_ID, differentUserId));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void cancelPaymentByOrderStatusKafkaMessage_WhenValidRequest_ShouldCancel() {
        // Arrange
        testPayment.setPaymentStatus(PaymentConstants.PaymentStatus.INSUFFICIENT_FUNDS);
        when(paymentRepository.findByOrderId(TEST_ORDER_ID)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        paymentService.cancelPaymentByOrderStatusKafkaMessage(TEST_ORDER_ID.toString());

        // Assert
        assertEquals(PaymentConstants.PaymentStatus.USER_CANCELED, testPayment.getPaymentStatus());
        verify(paymentRepository).save(testPayment);
    }

    @Test
    void validatePaymentData_WhenInvalidCard_ShouldThrowException() {
        // Arrange
        validPaymentRequest.setPaymentCard("123"); // Invalid card

        // Act & Assert
        assertThrows(InvalidPaymentException.class,
                () -> paymentService.createPayment(validPaymentRequest, "test-key", TEST_USER_ID));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void validatePaymentData_WhenInvalidCvv_ShouldThrowException() {
        // Arrange
        validPaymentRequest.setCvv("12"); // Invalid CVV

        // Act & Assert
        assertThrows(InvalidPaymentException.class,
                () -> paymentService.createPayment(validPaymentRequest, "test-key", TEST_USER_ID));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void validatePaymentData_WhenInvalidZip_ShouldThrowException() {
        // Arrange
        validPaymentRequest.setZip("123"); // Invalid ZIP

        // Act & Assert
        assertThrows(InvalidPaymentException.class,
                () -> paymentService.createPayment(validPaymentRequest, "test-key", TEST_USER_ID));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }
}