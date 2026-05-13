package com.connecthub.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StompPayload {

    private PayloadType type;
    private String senderId;
    private String senderUsername;
    private String senderFullName;
    private String senderAvatarUrl;
    private String roomId;
    private String content;
    private String mediaUrl;
    private String messageId;
    private String emoji;
    private String replyToId;
    @com.fasterxml.jackson.annotation.JsonProperty("isPinned")
    private boolean isPinned;
    private Map<String, Object> metadata;

    public enum PayloadType {
        CHAT_MESSAGE,
        TYPING_INDICATOR,
        READ_RECEIPT,
        REACTION,
        PRESENCE_UPDATE,
        MESSAGE_EDIT,
        MESSAGE_DELETE,
        MESSAGE_PIN,
        IMAGE,
        VIDEO,
        FILE
    }
}
