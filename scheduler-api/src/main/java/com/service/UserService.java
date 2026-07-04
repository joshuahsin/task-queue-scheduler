package com.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.DAO.UserDAO;
import com.entity.User;
import com.enums.Enums.UserRole;
import com.exception.InvalidCredentialsException;
import com.exception.UserNotFoundException;
import com.repo.UserRepo;
import com.security.JwtUtil;

@Service
public class UserService implements UserDAO {
    private final UserRepo userRepo;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepo userRepo, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public User register(UUID tenantId, String email, String password, UserRole role) {
        User user = new User();
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setCreatedAt(Instant.now());
        return userRepo.save(user);
    }

    @Override
    public String login(String email, String password) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return generateUserToken(user);
    }

    @Override
    public boolean deleteAccount(UUID userId, UUID tenantId) {
        return userRepo.findById(userId)
                .filter(user -> user.getTenantId().equals(tenantId))
                .map(user -> {
                    userRepo.delete(user);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public String refreshToken(String refreshToken) {
        UUID userId = UUID.fromString(jwtUtil.parseSubject(refreshToken));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return generateUserToken(user);
    }

    private String generateUserToken(User user) {
        return jwtUtil.generateToken(user.getUserId().toString(), Map.of(
                "tenantId", user.getTenantId().toString(),
                "role", user.getRole().name()
        ));
    }
}
