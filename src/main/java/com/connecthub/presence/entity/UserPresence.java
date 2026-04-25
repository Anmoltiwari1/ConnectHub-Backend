package com.connecthub.presence.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_presence")
@Data
public class UserPresence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String presenceId;

    private String userId;
    private String username;

    @Enumerated(EnumType.STRING)
    private PresenceStatus status = PresenceStatus.ONLINE;

    private String customMessage;
    private String deviceType;
    private String ipAddress;
    private String sessionId;
    private LocalDateTime connectedAt;
    private LocalDateTime lastPingAt;

    public enum PresenceStatus {
        ONLINE, AWAY, DND, INVISIBLE, OFFLINE
    }
}
