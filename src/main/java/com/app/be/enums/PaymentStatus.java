package com.app.be.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PaymentStatus {
    UNPAID,
    PAID,
    REFUNDED;

    @JsonCreator
    public static PaymentStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (PaymentStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Trạng thái thanh toán '" + value + "' không hợp lệ. Chỉ chấp nhận: UNPAID, PAID, REFUNDED");
    }
}
