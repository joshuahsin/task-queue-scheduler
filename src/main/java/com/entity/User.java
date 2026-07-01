package com.entity;

import java.time.Instant;
import java.util.UUID;

import com.enums.Enums.UserRole;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private UUID userId;
    private UUID tenantId;
    private String email;
    private String passwordHash;
    private UserRole role;
    private Instant createdAt;
}
