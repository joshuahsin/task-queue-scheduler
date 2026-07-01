package com.DAO;

import java.util.UUID;

import com.entity.User;
import com.enums.Enums.UserRole;

public interface UserDAO {
    public User register(UUID tenantId, String email, String password, UserRole role);
    public String login(String email, String password);
    public boolean deleteAccount(UUID userId);
    public String refreshToken(String refreshToken);
}
