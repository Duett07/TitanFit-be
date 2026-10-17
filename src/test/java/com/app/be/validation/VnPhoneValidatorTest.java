package com.app.be.validation;

import com.app.be.dto.auth.RegisterRequest;
import com.app.be.dto.order.CreateOrderRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class VnPhoneValidatorTest {

    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0901234567",
            "0387654321",
            "0561234567",
            "0771234567",
            "0888888888",
            "+84901234567",
            "+84387654321",
            "+84771234567"
    })
    @DisplayName("Should accept valid Vietnamese phone numbers")
    void isValid_validPhones_returnsTrue(String phone) {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@titanfit.vn")
                .password("Password123")
                .fullName("Test User")
                .phone(phone)
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should allow null or empty phone in RegisterRequest (optional field)")
    void isValid_nullOrEmptyPhoneInRegister_returnsTrue() {
        RegisterRequest requestWithNull = RegisterRequest.builder()
                .email("test@titanfit.vn")
                .password("Password123")
                .fullName("Test User")
                .phone(null)
                .build();

        assertThat(validator.validate(requestWithNull)).isEmpty();

        RegisterRequest requestWithEmpty = RegisterRequest.builder()
                .email("test@titanfit.vn")
                .password("Password123")
                .fullName("Test User")
                .phone("   ")
                .build();

        assertThat(validator.validate(requestWithEmpty)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0123456789",    // Obsolete 11-digit prefix 01
            "090123456",     // 9 digits
            "09012345678",   // 11 digits
            "1234567890",    // Does not start with 0 or +84
            "0241234567",    // Landline prefix
            "+840901234567", // Redundant 0 after +84
            "abcdefghij"     // Letters
    })
    @DisplayName("Should reject invalid phone numbers")
    void isValid_invalidPhones_returnsFalse(String invalidPhone) {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@titanfit.vn")
                .password("Password123")
                .fullName("Test User")
                .phone(invalidPhone)
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("phone"));
    }

    @Test
    @DisplayName("Should reject password shorter than 8 characters in RegisterRequest")
    void validatePassword_shorterThan8Chars_fails() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@titanfit.vn")
                .password("1234567") // 7 characters
                .fullName("Test User")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password")
                && v.getMessage().contains("Mật khẩu phải có độ dài từ 8 đến 72 ký tự"));
    }

    @Test
    @DisplayName("Should reject password longer than 72 characters in RegisterRequest")
    void validatePassword_longerThan72Chars_fails() {
        String longPassword = "a".repeat(73);
        RegisterRequest request = RegisterRequest.builder()
                .email("test@titanfit.vn")
                .password(longPassword)
                .fullName("Test User")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password")
                && v.getMessage().contains("Mật khẩu phải có độ dài từ 8 đến 72 ký tự"));
    }

    @Test
    @DisplayName("CreateOrderRequest requires non-blank phone and valid VN format")
    void createOrderRequest_phoneValidation() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerName("Nguyen Van A")
                .shippingAddress("123 Test St")
                .phone(null)
                .build();

        Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("phone")
                && v.getMessage().contains("Số điện thoại nhận hàng (phone) không được để trống"));
    }
}
