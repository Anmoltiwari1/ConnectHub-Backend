package com.connecthub.room.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_members")
@Data
public class RoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String memberId;

    private String roomId;
    private String userId;
    private String username;
    private String fullName;
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    private MemberRole role = MemberRole.MEMBER;

    private LocalDateTime joinedAt;
    private LocalDateTime lastReadAt;
    private boolean isMuted = false;

    @PrePersist
    void onCreate() {
        joinedAt = LocalDateTime.now();
    }

    public enum MemberRole {
        ADMIN, MEMBER
    }
}
