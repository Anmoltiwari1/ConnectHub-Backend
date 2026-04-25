package com.connecthub.message.repository;

import com.connecthub.message.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, String> {
    java.util.List<Reaction> findByMessage(com.connecthub.message.entity.Message message);
    java.util.Optional<Reaction> findByMessageAndUserIdAndEmoji(com.connecthub.message.entity.Message message, String userId, String emoji);
}
