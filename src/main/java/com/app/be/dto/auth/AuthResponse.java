package com.app.be.dto.auth;

import com.app.be.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse extends UserResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;

    public UserResponse getUser() {
        return UserResponse.builder()
                .id(this.getId())
                .email(this.getEmail())
                .fullName(this.getFullName())
                .phone(this.getPhone())
                .role(this.getRole())
                .createdAt(this.getCreatedAt())
                .build();
    }

    public static AuthResponse of(User user, String accessToken, String refreshToken, long expiresIn) {
        AuthResponse response = new AuthResponse();
        if (user != null) {
            response.setId(user.getId());
            response.setEmail(user.getEmail());
            response.setFullName(user.getFullName());
            response.setPhone(user.getPhone());
            response.setRole(user.getRole() != null ? user.getRole().name() : null);
            response.setCreatedAt(user.getCreatedAt());
        }
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(expiresIn);
        return response;
    }
}
