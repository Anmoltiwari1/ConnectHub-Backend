package com.connecthub.websocket.handler;

import com.connecthub.websocket.payload.StompPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * STOMP MESSAGE HANDLER
 * ---------------------
 * Purpose: This is the "Brain" of the real-time chat. 
 * When a user sends a message via WebSockets (STOMP), it arrives here.
 * This class handles:
 * 1. Authenticating the sender.
 * 2. Saving the message to the database (via Message Service).
 * 3. Sending the message to everyone else in the room.
 */
@Controller // Tells Spring this class handles incoming WebSocket messages
@RequiredArgsConstructor
@Slf4j
public class StompMessageHandler {

    // SimpMessagingTemplate is used to "push" messages from the server to specific clients
    private final SimpMessagingTemplate messagingTemplate;
    public static final String TOPIC_ROOM = "/topic/room/";
    private static final String MESSAGES_URL = "/messages/";
    
    // RestTemplate is used to talk to other microservices (like Message Service)
    private final org.springframework.web.client.RestTemplate restTemplate;
    
    // SimpUserRegistry helps us see who is currently connected to the WebSocket
    private final org.springframework.messaging.simp.user.SimpUserRegistry userRegistry;

    // The URL of the Message Service, defined in application.properties or defaulted
    @org.springframework.beans.factory.annotation.Value("${message.service.url:http://localhost:8083}")
    private String messageServiceUrl;

    // RabbitMQ support for asynchronous processing
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @org.springframework.beans.factory.annotation.Value("${connecthub.rabbitmq.exchange}")
    private String exchange;

    @org.springframework.beans.factory.annotation.Value("${connecthub.rabbitmq.routing-key}")
    private String routingKey;

    /**
     * HANDLE CHAT MESSAGE
     * Called when a user sends a message to "/app/chat.send"
     */
    @MessageMapping("/chat.send")
    public void handleChatMessage(@Payload StompPayload payload, java.security.Principal principal) {
        // Principal is the "Identity" of the user. We set this in the JwtChannelInterceptor.
        if (principal != null) {
            payload.setSenderId(principal.getName()); // Override senderId with the actual logged-in username
        }
        
        log.info("Received CHAT_MESSAGE from {} for room {}", payload.getSenderId(), payload.getRoomId());
        
        try {
            // STEP 1: Generate a unique messageId if not present (Decoupling from DB auto-gen)
            if (payload.getMessageId() == null) {
                payload.setMessageId(java.util.UUID.randomUUID().toString());
            }

            // STEP 2: Publish to RabbitMQ for asynchronous saving to database
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, payload);
                log.info("Published message {} to RabbitMQ", payload.getMessageId());
            } catch (Exception rabbitEx) {
                log.warn("RabbitMQ unavailable, falling back to REST for persistence: {}", rabbitEx.getMessage());
                // FALLBACK: Use REST to save the message directly
                String saveUrl = messageServiceUrl + "/messages";
                java.util.Map<String, Object> req = new java.util.HashMap<>();
                req.put("messageId", payload.getMessageId());
                req.put("roomId", payload.getRoomId());
                req.put("senderId", payload.getSenderId());
                req.put("senderUsername", payload.getSenderUsername());
                req.put("senderFullName", payload.getSenderFullName());
                req.put("senderAvatarUrl", payload.getSenderAvatarUrl());
                req.put("content", payload.getContent());
                req.put("type", payload.getType() != null ? payload.getType() : "TEXT");
                req.put("mediaUrl", payload.getMediaUrl());
                req.put("replyToMessageId", payload.getReplyToId());
                
                restTemplate.postForObject(saveUrl, req, Object.class);
            }
            
            // STEP 3: Broadcast (Push) the message to everyone subscribed to this room immediately
            messagingTemplate.convertAndSend(TOPIC_ROOM + payload.getRoomId(), payload);

            // STEP 4: Check for delivery status (Optional/Best effort)
            boolean isAnyOtherUserOnline = userRegistry.getUsers().stream()
                .anyMatch(user -> user.getSessions().stream()
                    .anyMatch(session -> session.getSubscriptions().stream()
                        .anyMatch(sub -> sub.getDestination().equals(TOPIC_ROOM + payload.getRoomId()))));
            
            if (isAnyOtherUserOnline) {
                String statusUrl = messageServiceUrl + MESSAGES_URL + payload.getMessageId() + "/status?status=DELIVERED";
                try { restTemplate.put(statusUrl, null); } catch(Exception ignored) {}
            }
        } catch (Exception e) {
            log.error("Failed to process chat message: {}", e.getMessage());
            // Last resort: broadcast anyway so the user sees their own message
            messagingTemplate.convertAndSend(TOPIC_ROOM + payload.getRoomId(), payload);
        }
    }

    /**
     * HANDLE TYPING INDICATOR
     * Called when a user starts or stops typing.
     */
    @MessageMapping("/chat.typing")
    public void handleTypingIndicator(@Payload StompPayload payload, java.security.Principal principal) {
        if (principal != null) {
            payload.setSenderId(principal.getName());
        }
        // We don't save typing indicators to the DB (too much data). 
        // We just broadcast them to the room so others see "X is typing..."
        messagingTemplate.convertAndSend(TOPIC_ROOM + payload.getRoomId(), payload);
    }

    /**
     * HANDLE READ RECEIPT
     * Called when a user views a message, turning the checkmarks blue.
     */
    @MessageMapping("/chat.read")
    public void handleReadReceipt(@Payload StompPayload payload, java.security.Principal principal) {
        if (principal != null) {
            payload.setSenderId(principal.getName());
        }
        
        try {
            // Tell the Message Service that all messages up to this ID are now read
            if (payload.getMessageId() != null) {
                String url = messageServiceUrl + "/messages/room/" + payload.getRoomId() + "/read?upToMessageId=" + payload.getMessageId();
                restTemplate.put(url, null);
                payload.setContent("READ");
            }
            
            // Tell all clients in the room to update their checkmark colors
            messagingTemplate.convertAndSend(TOPIC_ROOM + payload.getRoomId(), payload);
        } catch (Exception e) {
            log.error("Failed to update read status: {}", e.getMessage());
        }
    }

    /**
     * HANDLE REACTION
     * Called when a user adds an emoji (❤️, 😂, etc.) to a message.
     */
    @MessageMapping("/chat.reaction")
    public void handleReaction(@Payload StompPayload payload, java.security.Principal principal) {
        if (principal != null) {
            payload.setSenderId(principal.getName());
        }
        
        try {
            // Save the reaction to the database
            String url = messageServiceUrl + MESSAGES_URL + payload.getMessageId() + "/reactions";
            java.util.Map<String, String> req = new java.util.HashMap<>();
            req.put("userId", payload.getSenderId());
            req.put("emoji", payload.getEmoji());
            
            restTemplate.postForObject(url, req, Object.class);
            
            // Push the reaction to everyone so it appears on their screen instantly
            messagingTemplate.convertAndSend(TOPIC_ROOM + payload.getRoomId(), payload);
        } catch (Exception e) {
            log.error("Failed to persist reaction: {}", e.getMessage());
        }
    }

    /**
     * HANDLE MESSAGE DELETE
     * Called when a user deletes a message.
     */
    @MessageMapping("/chat.delete")
    public void handleMessageDelete(@Payload StompPayload payload, java.security.Principal principal) {
        if (principal != null) {
            payload.setSenderId(principal.getName());
        }
        log.info("🗑️ Received MESSAGE_DELETE for message {} in room {}", payload.getMessageId(), payload.getRoomId());

        try {
            // Async delete via RabbitMQ
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            } catch (Exception rabbitEx) {
                log.warn("RabbitMQ unavailable, falling back to REST for delete: {}", rabbitEx.getMessage());
                String url = messageServiceUrl + MESSAGES_URL + payload.getMessageId() + "?deleterId=" + payload.getSenderId();
                restTemplate.delete(url);
            }

            // Broadcast the deletion event to the room
            messagingTemplate.convertAndSend(TOPIC_ROOM + payload.getRoomId(), payload);
        } catch (Exception e) {
            log.error("Failed to delete message: {}", e.getMessage());
        }
    }

    /**
     * HANDLE MESSAGE PIN
     * Called when a user pins or unpins a message.
     */
    @MessageMapping("/chat.pin")
    public void handleMessagePin(@Payload StompPayload payload, java.security.Principal principal) {
        if (principal != null) {
            payload.setSenderId(principal.getName());
        }

        try {
            // Async pin via RabbitMQ
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            } catch (Exception rabbitEx) {
                log.warn("RabbitMQ unavailable, falling back to REST for pin: {}", rabbitEx.getMessage());
                String url = messageServiceUrl + MESSAGES_URL + payload.getMessageId() + "/pin?isPinned=" + payload.isPinned();
                restTemplate.put(url, null);
            }

            // Broadcast the pin event to the room
            messagingTemplate.convertAndSend(TOPIC_ROOM + payload.getRoomId(), payload);
        } catch (Exception e) {
            log.error("Failed to pin message: {}", e.getMessage());
        }
    }

    /**
     * HANDLE MESSAGE EDIT
     * Called when a user edits their message.
     */
    @MessageMapping("/chat.edit")
    public void handleMessageEdit(@Payload StompPayload payload, java.security.Principal principal) {
        if (principal != null) {
            payload.setSenderId(principal.getName());
        }

        try {
            // Async edit via RabbitMQ
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            } catch (Exception rabbitEx) {
                log.warn("RabbitMQ unavailable, falling back to REST for edit: {}", rabbitEx.getMessage());
                java.util.Map<String, String> req = new java.util.HashMap<>();
                req.put("editorId", payload.getSenderId());
                req.put("newContent", payload.getContent());
                restTemplate.put(messageServiceUrl + MESSAGES_URL + payload.getMessageId(), req);
            }

            // Broadcast the edit event to the room
            messagingTemplate.convertAndSend(TOPIC_ROOM + payload.getRoomId(), payload);
        } catch (Exception e) {
            log.error("Failed to edit message: {}", e.getMessage());
        }
    }
}
