package com.auth.controller;

import com.auth.dto.TokenResponse;
import com.auth.entity.Session;
import com.auth.entity.User;
import com.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    // ----------------- REGISTER -----------------
    @CrossOrigin(origins = "*")
    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        User savedUser = authService.register(user);
        return ResponseEntity.ok(savedUser);
    }

    // ----------------- LOGIN -----------------
    @CrossOrigin(origins = "*")
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
    public ResponseEntity<TokenResponse> refresh(@RequestParam String refreshToken) {
        TokenResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok(response);
    }

}
