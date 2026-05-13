package com.connecthub.room.resource;

import com.connecthub.room.entity.Room;
import com.connecthub.room.entity.Room.RoomType;
import com.connecthub.room.entity.RoomMember;
import com.connecthub.room.entity.RoomMember.MemberRole;
import com.connecthub.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomResource {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<Room> createRoom(@RequestBody Map<String, String> req) {
        return ResponseEntity.ok(roomService.createRoom(
                req.get("name"), req.get("description"),
                RoomType.valueOf(req.get("type")), req.get("createdById"),
                req.get("creatorName"),
                Boolean.parseBoolean(req.getOrDefault("isPrivate", "false"))));
    }

    @GetMapping
    public ResponseEntity<List<Room>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<Room> getRoom(@PathVariable String roomId) {
        return ResponseEntity.ok(roomService.getRoomById(roomId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Room>> getRoomsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(roomService.getRoomsByUser(userId));
    }

    @PutMapping("/{roomId}")
    public ResponseEntity<Room> updateRoom(@PathVariable String roomId, @RequestBody Map<String, String> req) {
        return ResponseEntity.ok(roomService.updateRoom(roomId,
                req.get("name"), req.get("description"), req.get("avatarUrl"),
                Integer.parseInt(req.getOrDefault("maxMembers", "0"))));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable String roomId) {
        roomService.deleteRoom(roomId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/members")
    public ResponseEntity<RoomMember> addMember(@PathVariable String roomId, @RequestBody Map<String, String> req) {
        return ResponseEntity.ok(roomService.addMember(roomId, req.get("userId"), req.get("username"),
                req.get("fullName"), req.get("avatarUrl"),
                MemberRole.valueOf(req.getOrDefault("role", "MEMBER"))));
    }

    @DeleteMapping("/{roomId}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable String roomId, @PathVariable String userId) {
        roomService.removeMember(roomId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{roomId}/members")
    public ResponseEntity<List<RoomMember>> getMembers(@PathVariable String roomId) {
        return ResponseEntity.ok(roomService.getMembers(roomId));
    }

    @PutMapping("/{roomId}/members/{userId}/role")
    public ResponseEntity<RoomMember> updateRole(@PathVariable String roomId, @PathVariable String userId,
                                                  @RequestParam MemberRole role) {
        return ResponseEntity.ok(roomService.updateMemberRole(roomId, userId, role));
    }

    @PutMapping("/{roomId}/members/{userId}/mute")
    public ResponseEntity<RoomMember> muteUnmute(@PathVariable String roomId, @PathVariable String userId,
                                                  @RequestParam boolean mute) {
        return ResponseEntity.ok(roomService.muteUnmuteMember(roomId, userId, mute));
    }

    @GetMapping("/{roomId}/unread/{userId}")
    public ResponseEntity<Long> getUnreadCount(@PathVariable String roomId, @PathVariable String userId) {
        return ResponseEntity.ok(roomService.getUnreadCount(roomId, userId));
    }

    @PostMapping("/{roomId}/read/{userId}")
    public ResponseEntity<Void> markAsRead(@PathVariable String roomId, @PathVariable String userId) {
        roomService.updateLastRead(roomId, userId);
        return ResponseEntity.ok().build();
    }
}
