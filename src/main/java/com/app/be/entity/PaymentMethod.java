package com.app.be.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PaymentMethod {
    COD,
    VIETQR;

    @JsonCreator
    public static PaymentMethod fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (PaymentMethod method : values()) {
            if (method.name().equalsIgnoreCase(value.trim())) {
                return method;
            }
        }
        throw new IllegalArgumentException("Phương thức thanh toán '" + value + "' không hợp lệ. Chỉ chấp nhận: COD hoặc VIETQR");
    }
}
