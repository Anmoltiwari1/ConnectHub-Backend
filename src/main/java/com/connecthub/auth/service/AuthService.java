package com.connecthub.auth.service;

import com.connecthub.auth.entity.User;
import com.connecthub.auth.entity.User.UserStatus;

import java.util.List;
import java.util.Map;

public interface AuthService {

    User register(String username, String email, String password, String fullName);
    Map<String, String> login(String email, String password);
    void logout(String userId);
    boolean validateToken(String token);
    Map<String, String> refreshToken(String refreshToken);
    User getUserById(String userId);
    User updateProfile(String userId, String fullName, String username, String avatarUrl, String bio);
    void changePassword(String userId, String currentPassword, String newPassword);
    List<User> searchUsers(String query);
    User updateStatus(String userId, UserStatus status);
    void recordLastSeen(String userId);
}
