package com.connecthub.websocket.handler;

import com.connecthub.websocket.payload.StompPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class StompMessageHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final org.springframework.web.client.RestTemplate restTemplate;
    private final org.springframework.messaging.simp.user.SimpUserRegistry userRegistry;

    @org.springframework.beans.factory.annotation.Value("${message.service.url:http://localhost:8083}")
    private String messageServiceUrl;

    @MessageMapping("/chat.send")
    public void handleChatMessage(@Payload StompPayload payload, java.security.Principal principal) {
        if (principal != null) {
            payload.setSenderId(principal.getName());
        }
        log.info("Received CHAT_MESSAGE from {} for room {}: {}", payload.getSenderId(), payload.getRoomId(), payload.getContent());
        
        try {
            // Persist message first to get messageId
            String saveUrl = messageServiceUrl + "/messages";
            java.util.Map<String, String> saveReq = new java.util.HashMap<>();
            saveReq.put("roomId", payload.getRoomId());
            saveReq.put("senderId", payload.getSenderId());
            saveReq.put("content", payload.getContent());
            saveReq.put("type", payload.getType() != null ? payload.getType().name() : "TEXT");
            saveReq.put("replyToMessageId", payload.getReplyToId());
            
            java.util.Map<String, Object> savedMessage = restTemplate.postForObject(saveUrl, saveReq, java.util.Map.class);
            String messageId = (String) savedMessage.get("messageId");
            payload.setMessageId(messageId);
            
            // Broadcast the saved message with its ID
            messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(), payload);

            // Check if any other users are online in this room to set DELIVERED status
            boolean isAnyOtherUserOnline = userRegistry.getUsers().stream()
                .anyMatch(user -> user.getSessions().stream()
                    .anyMatch(session -> session.getSubscriptions().stream()
                        .anyMatch(sub -> sub.getDestination().equals("/topic/room/" + payload.getRoomId()))));
            
            if (isAnyOtherUserOnline) {
                String statusUrl = messageServiceUrl + "/messages/" + messageId + "/status?status=DELIVERED";
                restTemplate.put(statusUrl, null);
                log.info("Message {} marked as DELIVERED", messageId);
                
                // Notify clients about the status update
                payload.setType(StompPayload.PayloadType.READ_RECEIPT); // Using READ_RECEIPT as generic status update event
                payload.setContent("DELIVERED");
                messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(), payload);
            }
        } catch (Exception e) {
            log.error("Failed to persist and broadcast message: {}", e.getMessage());
            // Fallback: broadcast original payload if save fails (degraded mode)
            messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(), payload);
        }
    }

    @MessageMapping("/chat.typing")
    public void handleTypingIndicator(@Payload StompPayload payload, java.security.Principal principal) {
        if (principal != null) {
            payload.setSenderId(principal.getName());
        }
        log.debug("Received TYPING_INDICATOR from {} for room {}", payload.getSenderId(), payload.getRoomId());
        // Broadcast typing signal
        messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(), payload);
    }

    @MessageMapping("/chat.read")
    public void handleReadReceipt(@Payload StompPayload payload, java.security.Principal principal) {
        if (principal != null) {
            payload.setSenderId(principal.getName());
        }
        log.info("Received READ_RECEIPT from {} for room {} up to {}", 
                payload.getSenderId(), payload.getRoomId(), payload.getMessageId());
        
        try {
            // Update status in message-service
            if (payload.getMessageId() != null) {
                String url = messageServiceUrl + "/messages/room/" + payload.getRoomId() + "/read?upToMessageId=" + payload.getMessageId();
                restTemplate.put(url, null);
                log.info("Messages in room {} marked as READ up to {}", payload.getRoomId(), payload.getMessageId());
                payload.setContent("READ");
            }
            
            // Broadcast read receipt to all room members
            messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(), payload);
        } catch (Exception e) {
            log.error("Failed to update read status: {}", e.getMessage());
        }
    }

    @MessageMapping("/chat.reaction")
    public void handleReaction(@Payload StompPayload payload, java.security.Principal principal) {
        if (principal != null) {
            payload.setSenderId(principal.getName());
        }
        log.info("Received REACTION from {} for message {}", payload.getSenderId(), payload.getMessageId());
        
        try {
            // Persist reaction via message-service
            String url = messageServiceUrl + "/messages/" + payload.getMessageId() + "/reactions";
            java.util.Map<String, String> req = new java.util.HashMap<>();
            req.put("userId", payload.getSenderId());
            req.put("emoji", payload.getEmoji());
            
            restTemplate.postForObject(url, req, Object.class);
            log.info("Reaction persisted successfully for message {}", payload.getMessageId());
            
            // Broadcast reaction to all room subscribers
            messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(), payload);
        } catch (Exception e) {
            log.error("Failed to persist reaction: {}", e.getMessage());
            // Still broadcast to keep UI responsive, or handle error
            messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(), payload);
        }
    }
}
