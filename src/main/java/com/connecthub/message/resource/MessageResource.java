package com.connecthub.message.resource;

import com.connecthub.message.dto.SendMessageRequest;
import com.connecthub.message.entity.Message;
import com.connecthub.message.entity.Message.DeliveryStatus;
import com.connecthub.message.entity.Message.MessageType;
import com.connecthub.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageResource {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<Message> sendMessage(@RequestBody Map<String, Object> req) {
        SendMessageRequest request = SendMessageRequest.builder()
                .messageId((String) req.get("messageId"))
                .roomId((String) req.get("roomId"))
                .senderId((String) req.get("senderId"))
                .senderUsername((String) req.get("senderUsername"))
                .senderFullName((String) req.get("senderFullName"))
                .senderAvatarUrl((String) req.get("senderAvatarUrl"))
                .content((String) req.get("content"))
                .type(req.get("type") != null ? MessageType.valueOf(req.get("type").toString()) : MessageType.TEXT)
                .mediaUrl((String) req.get("mediaUrl"))
                .replyToMessageId((String) req.get("replyToMessageId"))
                .build();
        
        return ResponseEntity.ok(messageService.sendMessage(request));
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<Message> getMessage(@PathVariable String messageId) {
        return ResponseEntity.ok(messageService.getMessageById(messageId));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<Page<Message>> getMessagesByRoom(@PathVariable String roomId,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(messageService.getMessagesByRoom(roomId, PageRequest.of(page, size)));
    }

    @PutMapping("/{messageId}")
    public ResponseEntity<Message> editMessage(@PathVariable String messageId, @RequestBody Map<String, String> req) {
        return ResponseEntity.ok(messageService.editMessage(messageId, req.get("editorId"), req.get("newContent")));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable String messageId, @RequestParam String deleterId) {
        messageService.deleteMessage(messageId, deleterId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{messageId}/pin")
    public ResponseEntity<Message> pinMessage(@PathVariable String messageId, @RequestParam boolean isPinned) {
        return ResponseEntity.ok(messageService.pinMessage(messageId, isPinned));
    }

    @GetMapping("/room/{roomId}/search")
    public ResponseEntity<List<Message>> searchMessages(@PathVariable String roomId, @RequestParam String keyword) {
        return ResponseEntity.ok(messageService.searchMessages(roomId, keyword));
    }

    @PutMapping("/{messageId}/status")
    public ResponseEntity<Message> updateStatus(@PathVariable String messageId, @RequestParam DeliveryStatus status) {
        return ResponseEntity.ok(messageService.updateDeliveryStatus(messageId, status));
    }

    @GetMapping("/room/{roomId}/count")
    public ResponseEntity<Long> getCount(@PathVariable String roomId) {
        return ResponseEntity.ok(messageService.getMessageCount(roomId));
    }

    @GetMapping("/room/{roomId}/unread")
    public ResponseEntity<Long> getUnreadCount(@PathVariable String roomId, @RequestParam(required = false) String since) {
        java.time.LocalDateTime sinceTime = since != null ? java.time.LocalDateTime.parse(since) : null;
        return ResponseEntity.ok(messageService.getUnreadCount(roomId, sinceTime));
    }

    @PostMapping("/{messageId}/reactions")
    public ResponseEntity<Message> addReaction(@PathVariable String messageId, @RequestBody Map<String, String> req) {
        return ResponseEntity.ok(messageService.addReaction(messageId, req.get("userId"), req.get("emoji")));
    }

    @PutMapping("/room/{roomId}/delivered")
    public ResponseEntity<Void> markRoomDelivered(@PathVariable String roomId) {
        messageService.markRoomAsDelivered(roomId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/room/{roomId}/read")
    public ResponseEntity<Void> markRoomRead(@PathVariable String roomId, @RequestParam String upToMessageId) {
        messageService.markRoomAsRead(roomId, upToMessageId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/room/{roomId}")
    public ResponseEntity<Void> deleteMessagesByRoom(@PathVariable String roomId) {
        messageService.deleteMessagesByRoom(roomId);
        return ResponseEntity.ok().build();
    }
}
