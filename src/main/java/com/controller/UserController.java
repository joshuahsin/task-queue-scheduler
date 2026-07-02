package com.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.DTO.AuthResponse;
import com.DTO.LoginRequest;
import com.DTO.RegisterRequest;
import com.service.UserService;

@RestController
@RequestMapping("/api/v1/auth")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request.getTenantId(), request.getEmail(), request.getPassword(), request.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(new AuthResponse(token, "Bearer"));
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(@RequestParam UUID userId) {
        userService.deleteAccount(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestParam String refreshToken) {
        String newToken = userService.refreshToken(refreshToken);
        return ResponseEntity.ok(new AuthResponse(newToken, "Bearer"));
    }
}
