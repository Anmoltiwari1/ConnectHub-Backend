package com.connecthub.presence.service;

import com.connecthub.presence.entity.UserPresence;
import com.connecthub.presence.entity.UserPresence.PresenceStatus;

import java.util.List;

public interface PresenceService {

    UserPresence setOnline(String userId, String username, String sessionId, String deviceType, String ipAddress);
    void setOffline(String userId);
    UserPresence updateStatus(String userId, PresenceStatus status, String customMessage);
    UserPresence getPresence(String userId);
    List<UserPresence> getBulkPresence(List<String> userIds);
    List<UserPresence> getOnlineUsers();
    void pingSession(String sessionId);
    long getOnlineCount();
    void cleanStaleSessions();
    boolean isOnline(String userId);
}
