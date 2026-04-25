package com.connecthub.auth.service;

import com.connecthub.auth.entity.User;
import com.connecthub.auth.entity.User.UserStatus;
import com.connecthub.auth.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @Override
    public User register(String username, String email, String password, String fullName) {
        if (userRepository.existsByEmail(email)) throw new IllegalArgumentException("Email already in use");
        if (userRepository.existsByUsername(username)) throw new IllegalArgumentException("Username taken");
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setProvider("LOCAL");
        return userRepository.save(user);
    }

    @Override
    public Map<String, String> login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(password, user.getPasswordHash()))
            throw new IllegalArgumentException("Invalid credentials");
        
        return Map.of(
            "token", generateToken(user.getUserId(), user.getUsername(), user.getEmail()),
            "username", user.getUsername(),
            "fullName", user.getFullName(),
            "userId", user.getUserId(),
            "email", user.getEmail()
        );
    }

    @Override
    public void logout(String userId) {
        recordLastSeen(userId);
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Map<String, String> refreshToken(String refreshToken) {
        if (!validateToken(refreshToken)) throw new IllegalArgumentException("Invalid refresh token");
        String userId = Jwts.parserBuilder().setSigningKey(getKey()).build()
                .parseClaimsJws(refreshToken).getBody().getSubject();
        User user = userRepository.findByUserId(userId).orElseThrow();
        return Map.of("token", generateToken(user.getUserId(), user.getUsername(), user.getEmail()));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(String userId) {
        return userRepository.findByUserId(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Override
    public User updateProfile(String userId, String fullName, String username, String avatarUrl, String bio) {
        User user = getUserById(userId);
        if (fullName != null) user.setFullName(fullName);
        if (username != null) user.setUsername(username);
        if (avatarUrl != null) user.setAvatarUrl(avatarUrl);
        if (bio != null) user.setBio(bio);
        return userRepository.save(user);
    }

    @Override
    public void changePassword(String userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash()))
            throw new IllegalArgumentException("Current password incorrect");
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> searchUsers(String query) {
        return userRepository.searchByUsername(query);
    }

    @Override
    public User updateStatus(String userId, UserStatus status) {
        User user = getUserById(userId);
        user.setStatus(status);
        return userRepository.save(user);
    }

    @Override
    public void recordLastSeen(String userId) {
        userRepository.findByUserId(userId).ifPresent(user -> {
            user.setLastSeenAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    private String generateToken(String userId, String username, String email) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("username", username)
                .claim("email", email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}
