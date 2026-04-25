package com.connecthub.presence.resource;

import com.connecthub.presence.entity.UserPresence;
import com.connecthub.presence.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/presence")
@RequiredArgsConstructor
public class PresenceResource {

    private final PresenceService presenceService;

    @PostMapping("/online")
    public ResponseEntity<UserPresence> setOnline(@RequestBody Map<String, String> req) {
        return ResponseEntity.ok(presenceService.setOnline(
                req.get("userId"), req.get("username"), req.get("sessionId"),
                req.get("deviceType"), req.get("ipAddress")));
    }

    @PostMapping("/offline/{userId}")
    public ResponseEntity<Void> setOffline(@PathVariable String userId) {
        presenceService.setOffline(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/online")
    public ResponseEntity<List<UserPresence>> getOnlineUsers() {
        return ResponseEntity.ok(presenceService.getOnlineUsers());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<UserPresence> getPresence(@PathVariable String userId) {
        return ResponseEntity.ok(presenceService.getPresence(userId));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getOnlineCount() {
        return ResponseEntity.ok(presenceService.getOnlineCount());
    }
}
