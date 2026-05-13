package com.connecthub.message.dto;

import com.connecthub.message.entity.Message.MessageType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SendMessageRequest {
    private String messageId;
    private String roomId;
    private String senderId;
    private String senderUsername;
    private String senderFullName;
    private String senderAvatarUrl;
    private String content;
    private MessageType type;
    private String mediaUrl;
    private String replyToMessageId;
}
