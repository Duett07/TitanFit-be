package com.app.be.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    private String phone;
    private String phoneNumber;
    private String gender;
    private String dateOfBirth;
    private String avatarUrl;
}
