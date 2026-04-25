package com.connecthub.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String userId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    private String passwordHash;
    private String fullName;
    private String avatarUrl;
    private String bio;

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ONLINE;

    private String provider; // LOCAL, GOOGLE, GITHUB

    private boolean isActive = true;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum UserStatus {
        ONLINE, AWAY, DND, INVISIBLE
    }
}
