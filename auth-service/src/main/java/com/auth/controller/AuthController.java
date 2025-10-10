package com.auth.controller;

import com.auth.entity.Session;
import com.auth.entity.User;
import com.auth.service.AuthService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        return ResponseEntity.ok(authService.register(user));
    }

    @PostMapping("/login")
    public ResponseEntity<Session> login(@RequestParam String username,
                                         @RequestParam String password) {
        return ResponseEntity.ok(authService.login(username, password));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Session> refresh(@RequestParam String refreshToken) {
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public void logout(@RequestParam Long userId) {
        User user = new User();
        user.setUserId(userId);
        authService.logout(user);
    }

    @Data
    static class RegisterRequest {
        private String username;
        private String password;
        private String email;
    }

    @Data
    static class LoginRequest {
        private String username;
        private String password;
    }
}
