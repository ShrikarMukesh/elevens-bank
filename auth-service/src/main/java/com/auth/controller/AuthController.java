package com.auth.controller;

import com.auth.entity.Session;
import com.auth.entity.User;
import com.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ----------------- REGISTER -----------------
    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        User registeredUser = authService.register(user);
        return ResponseEntity.ok(registeredUser);
    }

    // ----------------- LOGIN -----------------
    @PostMapping("/login")
    public ResponseEntity<Session> login(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request) {

        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        Session session = authService.login(username, password, ipAddress, userAgent);
        return ResponseEntity.ok(session);
    }

    // ----------------- LOGOUT -----------------
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestParam String username) {
        User user = authService.getUserByUsername(username);
        authService.logout(user);
        return ResponseEntity.ok("Logged out successfully");
    }

    // ----------------- REFRESH -----------------
    @PostMapping("/refresh")
    public ResponseEntity<Session> refresh(@RequestParam String refreshToken) {
        Session session = authService.refresh(refreshToken);
        return ResponseEntity.ok(session);
    }
}

