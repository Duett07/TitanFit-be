package com.app.be.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation kiểm tra tính hợp lệ của số điện thoại Việt Nam.
 * Áp dụng cho đầu số 03, 05, 07, 08, 09 hoặc tiền tố quốc tế +84 (10 chữ số).
 * Cho phép chuỗi rỗng hoặc null (để hỗ trợ trường không bắt buộc). Kết hợp với @NotBlank nếu cần bắt buộc.
 */
@Documented
@Constraint(validatedBy = VnPhoneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface VnPhone {

    String message() default "Số điện thoại không đúng định dạng Việt Nam (VD: 0912345678 hoặc +84912345678)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
