package com.connecthub.message.service;

import com.connecthub.message.entity.Message;
import com.connecthub.message.entity.Message.DeliveryStatus;
import com.connecthub.message.entity.Message.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageService {

    Message sendMessage(String roomId, String senderId, String content, MessageType type,
                        String mediaUrl, String replyToMessageId);
    Message getMessageById(String messageId);
    Page<Message> getMessagesByRoom(String roomId, Pageable pageable);
    List<Message> getMessagesBefore(String roomId, LocalDateTime before);
    Message editMessage(String messageId, String editorId, String newContent);
    void deleteMessage(String messageId, String deleterId);
    List<Message> searchMessages(String roomId, String keyword);
    Message updateDeliveryStatus(String messageId, DeliveryStatus status);
    long getMessageCount(String roomId);
    long getUnreadCount(String roomId, LocalDateTime since);
    List<Message> getUnreadMessages(String roomId, LocalDateTime since);
    Message addReaction(String messageId, String userId, String emoji);
    void markRoomAsDelivered(String roomId);
    void markRoomAsRead(String roomId, String upToMessageId);
}
