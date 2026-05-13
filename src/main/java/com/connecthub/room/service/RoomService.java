package com.connecthub.room.service;

import com.connecthub.room.entity.Room;
import com.connecthub.room.entity.Room.RoomType;
import com.connecthub.room.entity.RoomMember;
import com.connecthub.room.entity.RoomMember.MemberRole;

import java.util.List;

public interface RoomService {

    Room createRoom(String name, String description, RoomType type, String createdById, String creatorName, boolean isPrivate);
    Room getRoomById(String roomId);
    List<Room> getAllRooms();
    List<Room> getRoomsByUser(String userId);
    Room updateRoom(String roomId, String name, String description, String avatarUrl, int maxMembers);
    void deleteRoom(String roomId);
    RoomMember addMember(String roomId, String userId, String username, String fullName, String avatarUrl, MemberRole role);
    void removeMember(String roomId, String userId);
    List<RoomMember> getMembers(String roomId);
    RoomMember updateMemberRole(String roomId, String userId, MemberRole role);
    RoomMember muteUnmuteMember(String roomId, String userId, boolean mute);
    void updateLastRead(String roomId, String userId);
    long getUnreadCount(String roomId, String userId);
}
