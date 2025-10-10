package com.auth.service;

import com.auth.dto.LoginResponse;
import com.auth.entity.Session;
import com.auth.entity.User;
import com.auth.repository.SessionRepository;
import com.auth.repository.UserRepository;
import com.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    // ----------------- REGISTER -----------------
    public User register(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
            throw new RuntimeException("Password cannot be empty");
        }

        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        return userRepository.save(user);
    }

    // ----------------- LOGIN -----------------
    public Session login(String username, String password, String ipAddress, String userAgent) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        // Revoke previous active sessions
        sessionRepository.findByUserAndIsRevokedFalse(user)
                .forEach(s -> {
                    s.setIsRevoked(true);
                    s.setRevokedAt(LocalDateTime.now());
                    sessionRepository.save(s);
                });

        String accessToken = jwtUtil.generateToken(username);
        String refreshToken = UUID.randomUUID().toString();

        Session session = Session.builder()
                .user(user)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiryTime(LocalDateTime.now().plusMinutes(15))
                .isRevoked(false)
                .build();

        return sessionRepository.save(session);
    }

    // ----------------- LOGOUT -----------------
    public void logout(User user) {
        sessionRepository.findByUserAndIsRevokedFalse(user)
                .forEach(s -> {
                    s.setIsRevoked(true);
                    s.setRevokedAt(LocalDateTime.now());
                    sessionRepository.save(s);
                });
    }

    // ----------------- REFRESH -----------------
    public Session refresh(String refreshToken) {
        Session session = sessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (Boolean.TRUE.equals(session.getIsRevoked())) {
            throw new RuntimeException("Token revoked");
        }

        String newAccessToken = jwtUtil.generateToken(session.getUser().getUsername());
        session.setAccessToken(newAccessToken);
        session.setExpiryTime(LocalDateTime.now().plusMinutes(15));

        return sessionRepository.save(session);
    }

    // ----------------- HELPER -----------------
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ----------------- SPRING SECURITY -----------------
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }
}

