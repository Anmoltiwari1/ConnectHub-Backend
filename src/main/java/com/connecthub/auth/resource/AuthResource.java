package com.connecthub.auth.resource;

import com.connecthub.auth.entity.User;
import com.connecthub.auth.entity.User.UserStatus;
import com.connecthub.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthResource {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody Map<String, String> req) {
        System.out.println("Registration attempt for email: " + req.get("email"));
        return ResponseEntity.ok(authService.register(
                req.get("username"), req.get("email"), req.get("password"), req.get("fullName")));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> req) {
        System.out.println("Login attempt for email: " + req.get("email"));
        return ResponseEntity.ok(authService.login(req.get("email"), req.get("password")));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam String userId) {
        authService.logout(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(@RequestParam String userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(@RequestParam String userId, @RequestBody Map<String, String> req) {
        return ResponseEntity.ok(authService.updateProfile(userId,
                req.get("fullName"), req.get("username"), req.get("avatarUrl"), req.get("bio")));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@RequestParam String userId, @RequestBody Map<String, String> req) {
        authService.changePassword(userId, req.get("currentPassword"), req.get("newPassword"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String query) {
        return ResponseEntity.ok(authService.searchUsers(query));
    }

    @PutMapping("/status")
    public ResponseEntity<User> updateStatus(@RequestParam String userId, @RequestParam UserStatus status) {
        return ResponseEntity.ok(authService.updateStatus(userId, status));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestBody Map<String, String> req) {
        return ResponseEntity.ok(authService.refreshToken(req.get("refreshToken")));
    }
}
