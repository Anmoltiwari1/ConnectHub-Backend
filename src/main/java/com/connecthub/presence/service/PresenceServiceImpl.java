package com.connecthub.presence.service;

import com.connecthub.presence.entity.UserPresence;
import com.connecthub.presence.entity.UserPresence.PresenceStatus;
import com.connecthub.presence.repository.PresenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PresenceServiceImpl implements PresenceService {

    private final PresenceRepository presenceRepository;

    @Override
    public UserPresence setOnline(String userId, String username, String sessionId, String deviceType, String ipAddress) {
        UserPresence presence = presenceRepository.findByUserId(userId)
                .orElse(new UserPresence());
        presence.setUserId(userId);
        presence.setUsername(username);
        presence.setSessionId(sessionId);
        presence.setDeviceType(deviceType);
        presence.setIpAddress(ipAddress);
        presence.setStatus(PresenceStatus.ONLINE);
        presence.setConnectedAt(LocalDateTime.now());
        presence.setLastPingAt(LocalDateTime.now());
        return presenceRepository.save(presence);
    }

    @Override
    public void setOffline(String userId) {
        presenceRepository.findByUserId(userId).ifPresent(presence -> {
            presence.setStatus(PresenceStatus.OFFLINE);
            presenceRepository.save(presence);
        });
    }

    @Override
    public UserPresence updateStatus(String userId, PresenceStatus status, String customMessage) {
        UserPresence presence = presenceRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Presence not found"));
        presence.setStatus(status);
        presence.setCustomMessage(customMessage);
        return presenceRepository.save(presence);
    }

    @Override
    @Transactional(readOnly = true)
    public UserPresence getPresence(String userId) {
        return presenceRepository.findByUserId(userId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPresence> getBulkPresence(List<String> userIds) {
        return presenceRepository.findByUserIdIn(userIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPresence> getOnlineUsers() {
        return presenceRepository.findByStatus(PresenceStatus.ONLINE);
    }

    @Override
    public void pingSession(String sessionId) {
        presenceRepository.findBySessionId(sessionId).ifPresent(presence -> {
            presence.setLastPingAt(LocalDateTime.now());
            presenceRepository.save(presence);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public long getOnlineCount() {
        return presenceRepository.findOnlineUsers().size();
    }

    @Override
    @Scheduled(fixedRate = 60000)
    public void cleanStaleSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(60);
        List<UserPresence> stale = presenceRepository.findStaleSessions(threshold);
        stale.forEach(presence -> {
            presence.setStatus(PresenceStatus.OFFLINE);
            presenceRepository.save(presence);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOnline(String userId) {
        return presenceRepository.findByUserId(userId)
                .map(p -> p.getStatus() == PresenceStatus.ONLINE)
                .orElse(false);
    }
}
