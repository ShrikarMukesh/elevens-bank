package com.auth.service;

import com.auth.entity.Session;
import com.auth.entity.User;
import com.auth.kafka.EventPublisherService;
import com.auth.kafka.UserCreatedEvent;
import com.auth.repository.SessionRepository;
import com.auth.repository.UserRepository;
import com.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    private final EventPublisherService eventPublisherService;

    // ----------------- REGISTER -----------------
    public User register(User user) {
        // 1. Basic validation
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
            throw new RuntimeException("Password cannot be empty");
        }

        // 2. Encode password and persist
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        User savedUser = userRepository.save(user);

        // 3. Publish UserCreatedEvent
        UserCreatedEvent event = new UserCreatedEvent(
                savedUser.getUserId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getPhone(),
                LocalDateTime.now()
        );

        // Publish asynchronously via centralized service
        eventPublisherService.publishEvent("bank.user.event.v1", String.valueOf(event.getUserId()), event);
        return savedUser;
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

        String accessToken = jwtUtil.generateToken(username, user.getRole().name());
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

        String newAccessToken = jwtUtil.generateToken(session.getUser().getUsername(),
                session.getUser().getRole().name());
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
