package com.connecthub.message.service;

import com.connecthub.message.dto.SendMessageRequest;
import com.connecthub.message.entity.Message;
import com.connecthub.message.entity.Message.MessageType;
import com.connecthub.message.repository.MessageRepository;
import com.connecthub.message.repository.ReactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ReactionRepository reactionRepository;

    @InjectMocks
    private MessageServiceImpl messageService;

    @Test
    void sendMessage_Success() {
        Message message = new Message();
        message.setSenderId("user-1");
        message.setContent("Hello World!");

        when(messageRepository.save(any(Message.class))).thenReturn(message);

        Message result = messageService.sendMessage(SendMessageRequest.builder()
                .roomId("room-1")
                .senderId("user-1")
                .senderUsername("testuser")
                .senderFullName("Test User")
                .senderAvatarUrl(null)
                .content("Hello world")
                .type(MessageType.TEXT)
                .mediaUrl(null)
                .replyToMessageId(null)
                .build());

        assertNotNull(result);
        assertEquals("Hello World!", result.getContent());
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void getMessageById_Success() {
        Message message = new Message();
        message.setMessageId("msg-1");
        when(messageRepository.findById("msg-1")).thenReturn(Optional.of(message));

        Message result = messageService.getMessageById("msg-1");

        assertNotNull(result);
        assertEquals("msg-1", result.getMessageId());
    }

    @Test
    void deleteMessage_Success() {
        Message message = new Message();
        message.setMessageId("msg-1");
        message.setSenderId("user-1");

        when(messageRepository.findById("msg-1")).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        messageService.deleteMessage("msg-1", "user-1");

        assertTrue(message.isDeleted());
        verify(messageRepository).save(message);
    }
}
