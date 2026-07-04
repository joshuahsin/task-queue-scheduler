package com.DTO;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.enums.Enums.UserRole;

@Getter
@NoArgsConstructor
public class RegisterRequest {
    @NotNull
    private UUID tenantId;
    @Email(message = "Email should be valid")
    @NotBlank
    private String email;
    @NotBlank
    private String password;
    @NotNull
    private UserRole role;
}
