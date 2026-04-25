package com.connecthub.room.repository;

import com.connecthub.room.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, String> {

    List<RoomMember> findByRoomId(String roomId);
    Optional<RoomMember> findByUserIdAndRoomId(String userId, String roomId);
    long countByRoomId(String roomId);
    void deleteByUserIdAndRoomId(String userId, String roomId);
}
