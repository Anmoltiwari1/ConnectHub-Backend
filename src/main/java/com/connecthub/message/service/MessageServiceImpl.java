package com.connecthub.message.service;

import com.connecthub.message.entity.Message;
import com.connecthub.message.entity.Message.DeliveryStatus;
import com.connecthub.message.entity.Message.MessageType;
import com.connecthub.message.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final com.connecthub.message.repository.ReactionRepository reactionRepository;

    @Override
    public Message sendMessage(String roomId, String senderId, String content, MessageType type,
                               String mediaUrl, String replyToMessageId) {
        Message message = new Message();
        message.setRoomId(roomId);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setType(type);
        message.setMediaUrl(mediaUrl);
        message.setReplyToMessageId(replyToMessageId);
        return messageRepository.save(message);
    }

    @Override
    @Transactional(readOnly = true)
    public Message getMessageById(String messageId) {
        return messageRepository.findByMessageId(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Message> getMessagesByRoom(String roomId, Pageable pageable) {
        return messageRepository.findByRoomIdOrderBySentAtDesc(roomId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getMessagesBefore(String roomId, LocalDateTime before) {
        return messageRepository.findByRoomIdAndSentAtAfter(roomId, before);
    }

    @Override
    public Message editMessage(String messageId, String editorId, String newContent) {
        Message message = getMessageById(messageId);
        if (!message.getSenderId().equals(editorId)) throw new IllegalArgumentException("Not the sender");
        message.setContent(newContent);
        message.setEdited(true);
        message.setEditedAt(LocalDateTime.now());
        return messageRepository.save(message);
    }

    @Override
    public void deleteMessage(String messageId, String deleterId) {
        Message message = getMessageById(messageId);
        message.setDeleted(true);
        messageRepository.save(message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> searchMessages(String roomId, String keyword) {
        return messageRepository.searchInRoom(roomId, keyword);
    }

    @Override
    public Message updateDeliveryStatus(String messageId, DeliveryStatus status) {
        Message message = getMessageById(messageId);
        message.setDeliveryStatus(status);
        return messageRepository.save(message);
    }

    @Override
    @Transactional(readOnly = true)
    public long getMessageCount(String roomId) {
        return messageRepository.countByRoomId(roomId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String roomId, LocalDateTime since) {
        if (since == null) return messageRepository.countByRoomId(roomId);
        return messageRepository.countByRoomIdAndSentAtAfter(roomId, since);
    }

    @Override
    public List<Message> getUnreadMessages(String roomId, LocalDateTime since) {
        return messageRepository.findByRoomIdAndSentAtAfter(roomId, since);
    }

    @Override
    public Message addReaction(String messageId, String userId, String emoji) {
        Message message = getMessageById(messageId);
        
        // Toggle reaction: if exists, remove it; if not, add it
        java.util.Optional<com.connecthub.message.entity.Reaction> existing = 
                reactionRepository.findByMessageAndUserIdAndEmoji(message, userId, emoji);
        
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
            message.getReactions().removeIf(r -> r.getReactionId().equals(existing.get().getReactionId()));
        } else {
            com.connecthub.message.entity.Reaction reaction = new com.connecthub.message.entity.Reaction();
            reaction.setMessage(message);
            reaction.setUserId(userId);
            reaction.setEmoji(emoji);
            com.connecthub.message.entity.Reaction saved = reactionRepository.save(reaction);
            message.getReactions().add(saved);
        }
        
        return messageRepository.save(message);
    }

    @Override
    public void markRoomAsDelivered(String roomId) {
        List<Message> sentMessages = messageRepository.findByRoomIdAndSentAtAfter(roomId, LocalDateTime.now().minusDays(7));
        sentMessages.stream()
            .filter(m -> m.getDeliveryStatus() == DeliveryStatus.SENT)
            .forEach(m -> {
                m.setDeliveryStatus(DeliveryStatus.DELIVERED);
                messageRepository.save(m);
            });
    }

    @Override
    public void markRoomAsRead(String roomId, String upToMessageId) {
        Message lastMessage = getMessageById(upToMessageId);
        List<Message> messages = messageRepository.findByRoomIdAndSentAtAfter(roomId, LocalDateTime.now().minusDays(30));
        messages.stream()
            .filter(m -> m.getSentAt().isBefore(lastMessage.getSentAt()) || m.getSentAt().isEqual(lastMessage.getSentAt()))
            .filter(m -> m.getDeliveryStatus() != DeliveryStatus.READ)
            .forEach(m -> {
                m.setDeliveryStatus(DeliveryStatus.READ);
                messageRepository.save(m);
            });
    }
}
