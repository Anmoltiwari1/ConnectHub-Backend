package com.connecthub.room.service;

import com.connecthub.room.entity.Room;
import com.connecthub.room.entity.Room.RoomType;
import com.connecthub.room.entity.RoomMember;
import com.connecthub.room.entity.RoomMember.MemberRole;
import com.connecthub.room.repository.RoomMemberRepository;
import com.connecthub.room.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMemberRepository memberRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private RoomServiceImpl roomService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(roomService, "messageServiceUrl", "http://localhost:8083");
    }

    @Test
    void createRoom_Success() {
        Room room = new Room();
        room.setRoomId("room-1");
        room.setName("Test Room");

        when(roomRepository.save(any(Room.class))).thenReturn(room);

        RoomMember adminMember = new RoomMember();
        when(memberRepository.save(any(RoomMember.class))).thenReturn(adminMember);

        Room result = roomService.createRoom("Test Room", "Desc", RoomType.GROUP, "user-1", "Admin", false);

        assertNotNull(result);
        assertEquals("Test Room", result.getName());
        verify(roomRepository).save(any(Room.class));
        verify(memberRepository).save(any(RoomMember.class));
    }

    @Test
    void getRoomById_Success() {
        Room room = new Room();
        room.setRoomId("room-1");
        when(roomRepository.findByRoomId("room-1")).thenReturn(Optional.of(room));

        Room result = roomService.getRoomById("room-1");

        assertNotNull(result);
        assertEquals("room-1", result.getRoomId());
    }

    @Test
    void addMember_Success() {
        when(memberRepository.findByUserIdAndRoomId("user-2", "room-1")).thenReturn(Optional.empty());
        RoomMember newMember = new RoomMember();
        newMember.setUserId("user-2");
        when(memberRepository.save(any(RoomMember.class))).thenReturn(newMember);

        RoomMember result = roomService.addMember("room-1", "user-2", "username", "Test User", "avatar",
                MemberRole.MEMBER);

        assertNotNull(result);
        assertEquals("user-2", result.getUserId());
        verify(memberRepository).save(any(RoomMember.class));
    }
}
