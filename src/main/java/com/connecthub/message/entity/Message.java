package com.connecthub.message.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Data
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String messageId;

    private String roomId;
    private String senderId;
    private String senderUsername;
    private String senderFullName;
    private String senderAvatarUrl;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private MessageType type = MessageType.TEXT;

    private String mediaUrl;
    private String replyToMessageId;
    @com.fasterxml.jackson.annotation.JsonProperty("isEdited")
    private boolean isEdited = false;

    @com.fasterxml.jackson.annotation.JsonProperty("isDeleted")
    private boolean isDeleted = false;

    @com.fasterxml.jackson.annotation.JsonProperty("isPinned")
    private boolean isPinned = false;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus deliveryStatus = DeliveryStatus.SENT;

    private LocalDateTime sentAt;
    private LocalDateTime editedAt;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private java.util.List<Reaction> reactions = new java.util.ArrayList<>();

    @PrePersist
    void onCreate() {
        sentAt = LocalDateTime.now();
    }

    public enum MessageType {
        TEXT, IMAGE, VIDEO, FILE, REACTION, SYSTEM, CHAT_MESSAGE
    }

    public enum DeliveryStatus {
        SENT, DELIVERED, READ
    }
}
