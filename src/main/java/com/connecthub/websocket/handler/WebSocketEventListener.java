package com.connecthub.websocket.handler;

import com.connecthub.websocket.payload.StompPayload;
import com.connecthub.websocket.payload.StompPayload.PayloadType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
    private static final String PRESENCE_SERVICE_URL = "http://localhost:8085/presence";
    private static final String MESSAGE_SERVICE_URL = "http://localhost:8083/messages";
    
    // Map to track which session is in which room
    // SessionId -> RoomId
    private final Map<String, String> sessionRoomMap = new ConcurrentHashMap<>();

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        
        if (destination != null && destination.startsWith("/topic/room/")) {
            String roomId = destination.replace("/topic/room/", "");
            String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "Anonymous";
            String sessionId = headerAccessor.getSessionId();
            
            sessionRoomMap.put(sessionId, roomId);
            
            log.info("User {} joined room {}", username, roomId);
            
            StompPayload joinPayload = StompPayload.builder()
                    .type(PayloadType.PRESENCE_UPDATE)
                    .senderId(username)
                    .roomId(roomId)
                    .content(username + " joined the room")
                    .build();
            
            messagingTemplate.convertAndSend("/topic/room/" + roomId, joinPayload);

            // Notify Presence Service
            try {
                restTemplate.postForEntity(PRESENCE_SERVICE_URL + "/online", Map.of(
                    "userId", username,
                    "username", username,
                    "sessionId", sessionId,
                    "deviceType", "Web",
                    "ipAddress", "127.0.0.1"
                ), Map.class);
                
                // Mark room messages as DELIVERED for this user
                restTemplate.put(MESSAGE_SERVICE_URL + "/room/" + roomId + "/delivered", null);
                log.info("Marked room {} messages as DELIVERED for subscriber {}", roomId, username);
            } catch (Exception e) {
                log.error("Failed to notify Services: {}", e.getMessage());
            }
        }
    }

    @EventListener
    public void handleSessionDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "Anonymous";
        
        String roomId = sessionRoomMap.remove(sessionId);
        
        if (roomId != null) {
            log.info("User {} left room {}", username, roomId);
            
            StompPayload leavePayload = StompPayload.builder()
                    .type(PayloadType.PRESENCE_UPDATE)
                    .senderId(username)
                    .roomId(roomId)
                    .content(username + " left the room")
                    .build();
            
            messagingTemplate.convertAndSend("/topic/room/" + roomId, leavePayload);
        }
        
        // Notify Presence Service offline
        try {
            restTemplate.postForEntity(PRESENCE_SERVICE_URL + "/offline/" + username, null, Void.class);
        } catch (Exception e) {
            log.error("Failed to notify Presence Service (offline): {}", e.getMessage());
        }
    }
}
