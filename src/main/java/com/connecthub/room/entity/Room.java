package com.connecthub.room.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
@Data
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String roomId;

    private String name;
    private String description;

    @Enumerated(EnumType.STRING)
    private RoomType type;

    private String createdById;
    private String avatarUrl;
    private boolean isPrivate;
    private int maxMembers = 500;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum RoomType {
        GROUP, DM
    }
}
