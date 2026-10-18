package com.app.be.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPING,
    COMPLETED,
    CANCELLED;

    public Set<OrderStatus> nextAllowedStatuses() {
        return switch (this) {
            case PENDING -> EnumSet.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> EnumSet.of(SHIPPING, CANCELLED);
            case SHIPPING -> EnumSet.of(COMPLETED, CANCELLED);
            case COMPLETED, CANCELLED -> Collections.emptySet();
        };
    }

    public boolean canTransitionTo(OrderStatus next) {
        if (next == null) {
            return false;
        }
        return nextAllowedStatuses().contains(next);
    }

    @JsonCreator
    public static OrderStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (OrderStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Trạng thái đơn hàng '" + value + "' không hợp lệ. Chỉ chấp nhận: CONFIRMED, SHIPPING, COMPLETED, CANCELLED");
    }
}
