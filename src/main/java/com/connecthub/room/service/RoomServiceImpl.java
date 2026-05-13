package com.connecthub.room.service;

import com.connecthub.room.entity.Room;
import com.connecthub.room.entity.Room.RoomType;
import com.connecthub.room.entity.RoomMember;
import com.connecthub.room.entity.RoomMember.MemberRole;
import com.connecthub.room.repository.RoomMemberRepository;
import com.connecthub.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ROOM SERVICE IMPLEMENTATION
 * --------------------------
 * Purpose: This class contains the "Business Logic" for rooms.
 * It handles creating rooms, adding members, and keeping track of who is in
 * which room.
 * 
 * Target Audience: Beginners in Spring Boot & JPA.
 */
@Service // Tells Spring this is a Service bean (part of the business layer)
@RequiredArgsConstructor // Automatically creates a constructor for our repositories (Dependency
                         // Injection)
@Transactional // Ensures that database operations are safe (all or nothing)
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository; // Database access for Rooms
    private final RoomMemberRepository memberRepository; // Database access for Room Members
    private final org.springframework.web.client.RestTemplate restTemplate; // To talk to Message Service

    @org.springframework.beans.factory.annotation.Value("${message.service.url:http://localhost:8083}")
    private String messageServiceUrl;

    /**
     * CREATE ROOM
     * Logic: Create the room record, then automatically add the creator as the
     * 'ADMIN'.
     */
    // New room is created here.
    @Override
    public Room createRoom(String name, String description, RoomType type, String createdById, String creatorName,
            boolean isPrivate) {
        Room room = new Room();
        room.setName(name);
        room.setDescription(description);
        room.setType(type);
        room.setCreatedById(createdById);
        // DM (Direct Messages) are always private. Otherwise, use the user's choice.
        room.setPrivate(type == RoomType.DM || isPrivate);

        // Saved in db
        Room saved = roomRepository.save(room); // Persist to database

        // Every room needs at least one member (the creator)
        addMember(saved.getRoomId(), createdById, creatorName != null ? creatorName : "Admin", creatorName, null,
                MemberRole.ADMIN);

        return saved;
    }

    /**
     * GET ROOM BY ID
     * Logic: Fetch from DB or throw an error if it doesn't exist.
     */
    @Override
    @Transactional(readOnly = true)
    public Room getRoomById(String roomId) {
        return roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
    }

    /**
     * GET ALL ROOMS
     * Logic: Returns every room in the system. Used for room discovery.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    /**
     * GET ROOMS BY USER
     * Logic: Uses a custom SQL query to find rooms where this user is a member.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Room> getRoomsByUser(String userId) {
        return roomRepository.findRoomsByUserId(userId);
    }

    /**
     * UPDATE ROOM
     * Logic: Change room details (name, avatar, etc.)
     */
    @Override
    public Room updateRoom(String roomId, String name, String description, String avatarUrl, int maxMembers) {
        Room room = roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        if (name != null)
            room.setName(name);
        if (description != null)
            room.setDescription(description);
        if (avatarUrl != null)
            room.setAvatarUrl(avatarUrl);
        if (maxMembers > 0)
            room.setMaxMembers(maxMembers);
        return roomRepository.save(room);
    }

    /**
     * DELETE ROOM
     */
    @Override
    public void deleteRoom(String roomId) {
        // 1. Delete messages in message-service
        try {
            restTemplate.delete(messageServiceUrl + "/messages/room/" + roomId);
        } catch (Exception e) {
            // Log and continue
        }

        // 2. Delete members and the room itself
        memberRepository.deleteByRoomId(roomId);
        roomRepository.deleteById(roomId);
    }

    /**
     * ADD MEMBER
     * Logic: Checks if user is already a member. If not, creates a new membership.
     */
    // used to add a member to the room
    // when user joins the room in the DashboardPage.jsx
    @Override
    public RoomMember addMember(String roomId, String userId, String username, String fullName, String avatarUrl,
            MemberRole role) {
        // checks if the memebr in the db
        return memberRepository.findByUserIdAndRoomId(userId, roomId)
                .map(existing -> {
                    // Update info if it has changed
                    if (fullName != null) existing.setFullName(fullName);
                    if (avatarUrl != null) existing.setAvatarUrl(avatarUrl);
                    if (username != null) existing.setUsername(username);
                    return memberRepository.save(existing);
                })
                .orElseGet(() -> {
                    RoomMember member = new RoomMember();
                    member.setRoomId(roomId);
                    member.setUserId(userId);
                    member.setUsername(username);
                    member.setFullName(fullName);
                    member.setAvatarUrl(avatarUrl);
                    member.setRole(role);
                    return memberRepository.save(member);
                });
    }

    /**
     * REMOVE MEMBER (Leave Room)
     */
    @Override
    public void removeMember(String roomId, String userId) {
        memberRepository.deleteByUserIdAndRoomId(userId, roomId);
    }

    /**
     * GET MEMBERS
     * Logic: List all people currently in the room.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RoomMember> getMembers(String roomId) {
        return memberRepository.findByRoomId(roomId);
    }

    @Override
    public RoomMember updateMemberRole(String roomId, String userId, MemberRole role) {
        RoomMember member = memberRepository.findByUserIdAndRoomId(userId, roomId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        member.setRole(role);
        return memberRepository.save(member);
    }

    @Override
    public RoomMember muteUnmuteMember(String roomId, String userId, boolean mute) {
        RoomMember member = memberRepository.findByUserIdAndRoomId(userId, roomId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        member.setMuted(mute);
        return memberRepository.save(member);
    }

    /**
     * UPDATE LAST READ
     * Logic: When a user clicks a room, we update their 'lastReadAt' timestamp
     * so we can calculate unread messages later.
     */
    @Override
    public void updateLastRead(String roomId, String userId) {
        memberRepository.findByUserIdAndRoomId(userId, roomId).ifPresent(member -> {
            member.setLastReadAt(LocalDateTime.now());
            memberRepository.save(member);
        });
    }

    /**
     * GET UNREAD COUNT
     * Logic:
     * 1. Get the user's 'lastReadAt' time from our database.
     * 2. Call the Message Service to ask: "How many messages were sent in this room
     * since that time?"
     */
    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String roomId, String userId) {
        return memberRepository.findByUserIdAndRoomId(userId, roomId)
                .map(member -> {
                    String url = messageServiceUrl + "/messages/room/" + roomId + "/unread";
                    if (member.getLastReadAt() != null) {
                        url += "?since=" + member.getLastReadAt().toString();
                    }
                    try {
                        return restTemplate.getForObject(url, Long.class);
                    } catch (Exception e) {
                        return 0L;
                    }
                })
                .orElse(0L);
    }
}
