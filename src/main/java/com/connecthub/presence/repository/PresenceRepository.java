package com.connecthub.presence.repository;

import com.connecthub.presence.entity.UserPresence;
import com.connecthub.presence.entity.UserPresence.PresenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PresenceRepository extends JpaRepository<UserPresence, String> {

    Optional<UserPresence> findByUserId(String userId);
    List<UserPresence> findByStatus(PresenceStatus status);
    List<UserPresence> findByUserIdIn(List<String> userIds);
    Optional<UserPresence> findBySessionId(String sessionId);

    @Query("SELECT p FROM UserPresence p WHERE p.status = 'ONLINE'")
    List<UserPresence> findOnlineUsers();

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByUserId(String userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteBySessionId(String sessionId);

    @Query("SELECT p FROM UserPresence p WHERE p.lastPingAt < :threshold")
    List<UserPresence> findStaleSessions(LocalDateTime threshold);
}
