package com.connecthub.presence.service;

import com.connecthub.presence.entity.UserPresence;
import com.connecthub.presence.entity.UserPresence.PresenceStatus;
import com.connecthub.presence.repository.PresenceRepository;
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
class PresenceServiceImplTest {

    @Mock
    private PresenceRepository presenceRepository;

    @InjectMocks
    private PresenceServiceImpl presenceService;

    @Test
    void setOnline_Success() {
        when(presenceRepository.findByUserId("user-1")).thenReturn(Optional.empty());
        UserPresence presence = new UserPresence();
        presence.setUserId("user-1");
        presence.setStatus(PresenceStatus.ONLINE);
        when(presenceRepository.save(any(UserPresence.class))).thenReturn(presence);

        UserPresence result = presenceService.setOnline("user-1", "user1", "sess-1", "web", "127.0.0.1");

        assertNotNull(result);
        assertEquals(PresenceStatus.ONLINE, result.getStatus());
        verify(presenceRepository).save(any(UserPresence.class));
    }

    @Test
    void updateStatus_Success() {
        UserPresence presence = new UserPresence();
        presence.setUserId("user-1");
        when(presenceRepository.findByUserId("user-1")).thenReturn(Optional.of(presence));
        when(presenceRepository.save(any(UserPresence.class))).thenReturn(presence);

        UserPresence result = presenceService.updateStatus("user-1", PresenceStatus.AWAY, "BRB");

        assertEquals(PresenceStatus.AWAY, result.getStatus());
        assertEquals("BRB", result.getCustomMessage());
    }

    @Test
    void isOnline_ReturnsTrue() {
        UserPresence presence = new UserPresence();
        presence.setStatus(PresenceStatus.ONLINE);
        when(presenceRepository.findByUserId("user-1")).thenReturn(Optional.of(presence));

        boolean online = presenceService.isOnline("user-1");

        assertTrue(online);
    }
}
