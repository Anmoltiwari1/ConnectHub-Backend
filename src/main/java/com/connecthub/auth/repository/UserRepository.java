package com.connecthub.auth.repository;

import com.connecthub.auth.entity.User;
import com.connecthub.auth.entity.User.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByUserId(String userId);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    List<User> findByStatus(UserStatus status);

    @Query("SELECT u FROM User u WHERE u.username LIKE %:query%")
    List<User> searchByUsername(@Param("query") String query);

    void deleteByUserId(String userId);
}
