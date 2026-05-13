package com.connecthub.message.repository;

import com.connecthub.message.entity.Message;
import com.connecthub.message.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, String> {
    List<Reaction> findByMessage(Message message);
    Optional<Reaction> findByMessageAndUserIdAndEmoji(Message message, String userId, String emoji);
}
