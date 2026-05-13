package com.connecthub.room.repository;

import com.connecthub.room.entity.Room;
import com.connecthub.room.entity.Room.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, String> {

    Optional<Room> findByRoomId(String roomId);
    List<Room> findByCreatedById(String createdById);
    List<Room> findByType(RoomType type);

    @Query("SELECT DISTINCT r FROM Room r JOIN RoomMember m ON r.roomId = m.roomId WHERE m.userId = :userId ORDER BY r.lastMessageAt DESC")
    List<Room> findRoomsByUserId(@Param("userId") String userId);
}
