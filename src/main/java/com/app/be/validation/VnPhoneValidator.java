package com.app.be.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validator kiểm tra định dạng số điện thoại di động Việt Nam.
 * - Đầu số: 03, 05, 07, 08, 09 hoặc +843, +845, +847, +848, +849.
 * - Theo sau là đúng 8 chữ số (tổng cộng 10 số khi dùng đầu 0).
 */
public class VnPhoneValidator implements ConstraintValidator<VnPhone, String> {

    private static final Pattern VN_PHONE_PATTERN = Pattern.compile("^(0|\\+84)(3|5|7|8|9)[0-9]{8}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Cho phép giá trị null hoặc rỗng khi trường là optional.
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        return VN_PHONE_PATTERN.matcher(value.trim()).matches();
    }
}
