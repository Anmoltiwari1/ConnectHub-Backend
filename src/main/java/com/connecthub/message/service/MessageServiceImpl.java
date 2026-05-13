package com.connecthub.message.service;

import com.connecthub.message.dto.SendMessageRequest;
import com.connecthub.message.entity.Message;
import com.connecthub.message.entity.Message.DeliveryStatus;
import com.connecthub.message.entity.Message.MessageType;
import com.connecthub.message.entity.Reaction;
import com.connecthub.message.repository.MessageRepository;
import com.connecthub.message.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MESSAGE SERVICE IMPLEMENTATION
 * -----------------------------
 * Purpose: This class handles the actual chat data. 
 * It saves messages, handles edits, deletions, and emoji reactions.
 * It also tracks whether a message has been 'SENT', 'DELIVERED', or 'READ'.
 */
@Service // Tells Spring this class is a Service (business logic layer)
@RequiredArgsConstructor
@Transactional // Ensures database operations are reliable
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository; // Access to 'messages' table
    private final ReactionRepository reactionRepository; // Access to 'reactions' table

    /**
     * SEND MESSAGE
     * Logic: Create a new Message object and save it to the DB.
     */
    @Override
    public Message sendMessage(SendMessageRequest request) {
        Message message = new Message();
        if (request.getMessageId() != null) {
            message.setMessageId(request.getMessageId());
        }
        message.setRoomId(request.getRoomId());
        message.setSenderId(request.getSenderId());
        message.setSenderUsername(request.getSenderUsername());
        message.setSenderFullName(request.getSenderFullName());
        message.setSenderAvatarUrl(request.getSenderAvatarUrl());
        message.setContent(request.getContent());
        message.setType(request.getType()); // TEXT, IMAGE, VIDEO, etc.
        message.setMediaUrl(request.getMediaUrl());
        message.setReplyToMessageId(request.getReplyToMessageId()); // For threaded replies
        return messageRepository.save(message);
    }

    /**
     * GET MESSAGES BY ROOM
     * Logic: Fetches a "Page" of messages (e.g., first 50) sorted by newest first.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<Message> getMessagesByRoom(String roomId, Pageable pageable) {
        return messageRepository.findByRoomIdOrderBySentAtAsc(roomId, pageable);
    }

    /**
     * EDIT MESSAGE
     * Logic: Verify the editor is the original sender, then update the content.
     */
    @Override
    public Message editMessage(String messageId, String editorId, String newContent) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        // Security check: only the sender can edit their own message
        if (!message.getSenderId().equals(editorId)) throw new IllegalArgumentException("Not the sender");
        
        message.setContent(newContent);
        message.setEdited(true);
        message.setEditedAt(LocalDateTime.now());
        return messageRepository.save(message);
    }

    /**
     * GET UNREAD COUNT
     * Logic: Counts how many messages were sent after a certain timestamp (the 'since' time).
     */
    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String roomId, LocalDateTime since) {
        // If 'since' is null (never read), count all messages in the room
        if (since == null) return messageRepository.countByRoomId(roomId);
        return messageRepository.countByRoomIdAndSentAtAfter(roomId, since);
    }

    /**
     * ADD REACTION
     * Logic: Toggles an emoji on a message. If the user already added it, remove it. Otherwise, add it.
     */
    @Override
    public Message addReaction(String messageId, String userId, String emoji) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        // Search for an existing reaction by this user with this specific emoji
        java.util.Optional<Reaction> existing =
                reactionRepository.findByMessageAndUserIdAndEmoji(message, userId, emoji);

        if (existing.isPresent()) {
            // REMOVE existing reaction
            reactionRepository.delete(existing.get());
            message.getReactions().removeIf(r -> r.getReactionId().equals(existing.get().getReactionId()));
        } else {
            // ADD new reaction
            Reaction reaction = new Reaction();
            reaction.setMessage(message);
            reaction.setUserId(userId);
            reaction.setEmoji(emoji);
            Reaction saved = reactionRepository.save(reaction);
            message.getReactions().add(saved);
        }
        
        return messageRepository.save(message);
    }

    /**
     * MARK ROOM AS READ
     * Logic: Finds all messages in the room sent before or at the time of a specific message 
     * and sets their status to 'READ'.
     */
    @Override
    public void markRoomAsRead(String roomId, String upToMessageId) {
        Message lastMessage = messageRepository.findById(upToMessageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        List<Message> messages = messageRepository.findByRoomIdAndSentAtAfter(roomId, LocalDateTime.now().minusDays(30));
        
        messages.stream()
            .filter(m -> m.getSentAt().isBefore(lastMessage.getSentAt()) || m.getSentAt().isEqual(lastMessage.getSentAt()))
            .filter(m -> m.getDeliveryStatus() != DeliveryStatus.READ)
            .forEach(m -> {
                m.setDeliveryStatus(DeliveryStatus.READ);
                messageRepository.save(m);
            });
    }

    @Override
    public void markRoomAsDelivered(String roomId) {
        List<Message> messages = messageRepository.findByRoomIdAndSentAtAfter(roomId, LocalDateTime.now().minusDays(7));
        messages.stream()
            .filter(m -> m.getDeliveryStatus() == DeliveryStatus.SENT)
            .forEach(m -> {
                m.setDeliveryStatus(DeliveryStatus.DELIVERED);
                messageRepository.save(m);
            });
    }

    @Override
    public List<Message> getMessagesBefore(String roomId, LocalDateTime before) {
        return messageRepository.findByRoomIdAndSentAtBefore(roomId, before);
    }

    @Override
    public void deleteMessage(String messageId, String deleterId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        // Security check: only the sender can delete their own message
        // We check both senderId (UUID) and senderUsername to be robust
        boolean isOwner = (message.getSenderId() != null && message.getSenderId().equals(deleterId)) ||
                          (message.getSenderUsername() != null && message.getSenderUsername().equals(deleterId));
        
        if (!isOwner) throw new IllegalArgumentException("Not authorized to delete this message");
        
        message.setDeleted(true);
        messageRepository.save(message);
    }

    @Override
    public Message pinMessage(String messageId, boolean isPinned) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        message.setPinned(isPinned);
        return messageRepository.save(message);
    }

    @Override
    public List<Message> searchMessages(String roomId, String keyword) {
        return messageRepository.findByRoomIdAndContentContainingIgnoreCase(roomId, keyword);
    }

    @Override
    public Message updateDeliveryStatus(String messageId, DeliveryStatus status) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        message.setDeliveryStatus(status);
        return messageRepository.save(message);
    }

    @Override
    public long getMessageCount(String roomId) {
        return messageRepository.countByRoomId(roomId);
    }

    @Override
    public List<Message> getUnreadMessages(String roomId, LocalDateTime since) {
        if (since == null) return messageRepository.findByRoomIdAndSentAtAfter(roomId, LocalDateTime.now().minusDays(365));
        return messageRepository.findByRoomIdAndSentAtAfter(roomId, since);
    }

    // Helper method to fetch a message by ID
    @Override
    @Transactional(readOnly = true)
    public Message getMessageById(String messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
    }

    @Override
    public Message saveMessageWithId(String messageId, SendMessageRequest request) {
        // Use findByMessageId to check if it exists (for idempotency)
        return messageRepository.findById(messageId).orElseGet(() -> {
            Message message = new Message();
            message.setMessageId(messageId); // Set the ID manually
            message.setRoomId(request.getRoomId());
            message.setSenderId(request.getSenderId());
            message.setSenderUsername(request.getSenderUsername());
            message.setSenderFullName(request.getSenderFullName());
            message.setSenderAvatarUrl(request.getSenderAvatarUrl());
            message.setContent(request.getContent());
            message.setType(request.getType());
            message.setMediaUrl(request.getMediaUrl());
            message.setReplyToMessageId(request.getReplyToMessageId());
            return messageRepository.save(message);
        });
    }

    @Override
    public void deleteMessagesByRoom(String roomId) {
        messageRepository.deleteByRoomId(roomId);
    }
}
