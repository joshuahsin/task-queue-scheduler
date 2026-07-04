package com.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {
    @Email(message = "Email should be valid")
    @NotBlank
    private String email;
    @NotBlank
    private String password;
}
