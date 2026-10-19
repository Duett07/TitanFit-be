package com.app.be.dto.auth;

import com.app.be.dto.address.AddressResponse;
import com.app.be.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private String gender;
    private String dateOfBirth;
    private String role;
    private List<AddressResponse> addresses;
    private LocalDateTime createdAt;

    public static UserResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .addresses(user.getAddresses() != null
                        ? user.getAddresses().stream().map(AddressResponse::fromEntity).collect(Collectors.toList())
                        : Collections.emptyList())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
