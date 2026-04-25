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

@Service
@RequiredArgsConstructor
@Transactional
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;
    private final org.springframework.web.client.RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Value("${message.service.url:http://localhost:8083}")
    private String messageServiceUrl;

    @Override
    public Room createRoom(String name, String description, RoomType type, String createdById, String creatorName, boolean isPrivate) {
        Room room = new Room();
        room.setName(name);
        room.setDescription(description);
        room.setType(type);
        room.setCreatedById(createdById);
        room.setPrivate(type == RoomType.DM || isPrivate);
        Room saved = roomRepository.save(room);
        addMember(saved.getRoomId(), createdById, creatorName != null ? creatorName : "Admin", MemberRole.ADMIN);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Room getRoomById(String roomId) {
        return roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Room> getRoomsByUser(String userId) {
        return roomRepository.findRoomsByUserId(userId);
    }

    @Override
    public Room updateRoom(String roomId, String name, String description, String avatarUrl, int maxMembers) {
        Room room = getRoomById(roomId);
        if (name != null) room.setName(name);
        if (description != null) room.setDescription(description);
        if (avatarUrl != null) room.setAvatarUrl(avatarUrl);
        if (maxMembers > 0) room.setMaxMembers(maxMembers);
        return roomRepository.save(room);
    }

    @Override
    public void deleteRoom(String roomId) {
        roomRepository.deleteById(roomId);
    }

    @Override
    public RoomMember addMember(String roomId, String userId, String username, MemberRole role) {
        return memberRepository.findByUserIdAndRoomId(userId, roomId)
                .orElseGet(() -> {
                    RoomMember member = new RoomMember();
                    member.setRoomId(roomId);
                    member.setUserId(userId);
                    member.setUsername(username);
                    member.setRole(role);
                    return memberRepository.save(member);
                });
    }

    @Override
    public void removeMember(String roomId, String userId) {
        memberRepository.deleteByUserIdAndRoomId(userId, roomId);
    }

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

    @Override
    public void updateLastRead(String roomId, String userId) {
        memberRepository.findByUserIdAndRoomId(userId, roomId).ifPresent(member -> {
            member.setLastReadAt(LocalDateTime.now());
            memberRepository.save(member);
        });
    }

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
