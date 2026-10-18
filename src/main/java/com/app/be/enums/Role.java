package com.app.be.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Role {
    ROLE_CUSTOMER,
    ROLE_ADMIN;

    @JsonCreator
    public static Role fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized.toUpperCase();
        }
        for (Role role : values()) {
            if (role.name().equalsIgnoreCase(normalized)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Vai trò '" + value + "' không hợp lệ. Chỉ chấp nhận: ROLE_CUSTOMER hoặc ROLE_ADMIN");
    }
}
