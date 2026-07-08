package com.service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.entity.User;
import com.enums.Enums.UserRole;
import com.exception.InvalidCredentialsException;
import com.exception.UserNotFoundException;
import com.repo.UserRepo;
import com.security.JwtUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Mock
    private UserRepo userRepo;
    @Mock
    private JwtUtil jwtUtil;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepo, jwtUtil);
    }

    @Test
    void register_hashesPasswordAndSavesUser() {
        UUID tenantId = UUID.randomUUID();
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.register(tenantId, "a@b.com", "hunter2", UserRole.ADMIN);

        assertThat(user.getTenantId()).isEqualTo(tenantId);
        assertThat(user.getEmail()).isEqualTo("a@b.com");
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getPasswordHash()).isNotEqualTo("hunter2");
        assertThat(ENCODER.matches("hunter2", user.getPasswordHash())).isTrue();
    }

    @Test
    void login_returnsTokenWhenPasswordMatches() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        User user = new User();
        user.setUserId(userId);
        user.setTenantId(tenantId);
        user.setEmail("a@b.com");
        user.setPasswordHash(ENCODER.encode("hunter2"));
        user.setRole(UserRole.USER);
        when(userRepo.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("jwt-token");

        String token = userService.login("a@b.com", "hunter2");

        assertThat(token).isEqualTo("jwt-token");

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jwtUtil).generateToken(subjectCaptor.capture(), claimsCaptor.capture());
        assertThat(subjectCaptor.getValue()).isEqualTo(userId.toString());
        assertThat(claimsCaptor.getValue()).containsEntry("tenantId", tenantId.toString());
        assertThat(claimsCaptor.getValue()).containsEntry("role", "USER");
    }

    @Test
    void login_throwsWhenPasswordDoesNotMatch() {
        User user = new User();
        user.setPasswordHash(ENCODER.encode("hunter2"));
        when(userRepo.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login("a@b.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_throwsWhenUserNotFound() {
        when(userRepo.findByEmail("nobody@b.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login("nobody@b.com", "whatever"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void deleteAccount_deletesWhenTenantMatches() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        User user = new User();
        user.setUserId(userId);
        user.setTenantId(tenantId);
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));

        boolean result = userService.deleteAccount(userId, tenantId);

        assertThat(result).isTrue();
        verify(userRepo).delete(user);
    }

    @Test
    void deleteAccount_returnsFalseWhenTenantDoesNotMatch() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUserId(userId);
        user.setTenantId(UUID.randomUUID());
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));

        boolean result = userService.deleteAccount(userId, UUID.randomUUID());

        assertThat(result).isFalse();
        verify(userRepo, never()).delete(any());
    }

    @Test
    void refreshToken_issuesNewTokenForSubjectUser() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        User user = new User();
        user.setUserId(userId);
        user.setTenantId(tenantId);
        user.setRole(UserRole.ADMIN);
        when(jwtUtil.parseSubject("refresh-token")).thenReturn(userId.toString());
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("new-jwt-token");

        String token = userService.refreshToken("refresh-token");

        assertThat(token).isEqualTo("new-jwt-token");
    }

    @Test
    void refreshToken_throwsWhenUserNoLongerExists() {
        UUID userId = UUID.randomUUID();
        when(jwtUtil.parseSubject("refresh-token")).thenReturn(userId.toString());
        when(userRepo.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.refreshToken("refresh-token"))
                .isInstanceOf(UserNotFoundException.class);
    }
}
