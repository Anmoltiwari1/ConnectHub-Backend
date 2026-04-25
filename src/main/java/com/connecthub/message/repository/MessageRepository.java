package com.connecthub.message.repository;

import com.connecthub.message.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, String> {

    Optional<Message> findByMessageId(String messageId);
    Page<Message> findByRoomIdOrderBySentAtDesc(String roomId, Pageable pageable);
    List<Message> findBySenderId(String senderId);
    List<Message> findByRoomIdAndSentAtAfter(String roomId, LocalDateTime after);

    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId AND m.content LIKE %:keyword% AND m.isDeleted = false")
    List<Message> searchInRoom(@Param("roomId") String roomId, @Param("keyword") String keyword);

    long countByRoomId(String roomId);
    long countByRoomIdAndSentAtAfter(String roomId, LocalDateTime after);
    void deleteByMessageId(String messageId);
}
