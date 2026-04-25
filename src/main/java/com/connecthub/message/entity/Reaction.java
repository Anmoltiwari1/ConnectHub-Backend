package com.connecthub.message.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "reactions")
@Data
public class Reaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String reactionId;

    @ManyToOne
    @JoinColumn(name = "messageId")
    private Message message;

    private String userId;
    private String emoji;
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
