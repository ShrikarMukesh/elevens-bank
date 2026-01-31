package com.auth.service;

import com.auth.dto.TokenResponse;
import com.auth.entity.AuditLog;
import com.auth.entity.Session;
import com.auth.entity.User;
import com.auth.exception.BadRequestException;
import com.auth.exception.InvalidCredentialsException;
import com.auth.exception.ResourceNotFoundException;
import com.auth.kafka.EventPublisherService;
import com.auth.kafka.UserCreatedEvent;
import com.auth.repository.AuditLogRepository;
import com.auth.repository.SessionRepository;
import com.auth.repository.UserRepository;
import com.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final AuditLogRepository auditLogRepository; // Injected
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    private final EventPublisherService eventPublisherService;

    @Value("${spring.kafka.template.default-topic}")
    private String userEventsTopic;

    // ----------------- REGISTER -----------------
    public User register(User user) {
        // 1. Basic validation
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new BadRequestException("Username already exists");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
            throw new BadRequestException("Password cannot be empty");
        }

        // 2. Encode password and persist
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        User savedUser = userRepository.save(user);

        // Audit Log: USER_REGISTERED
        try {
            auditLogRepository.save(AuditLog.builder()
                    .serviceName("AUTH-SERVICE")
                    .eventType("USER_REGISTERED")
                    .userId(String.valueOf(savedUser.getUserId()))
                    .affectedEntityType("USER")
                    .affectedEntityId(String.valueOf(savedUser.getUserId()))
                    .description("User registered successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }

        // 3. Publish UserCreatedEvent
        UserCreatedEvent event = UserCreatedEvent.builder()
                .userId(String.valueOf(user.getUserId()))
                .username(user.getUsername())
                .fullName(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .build();

        // Publish asynchronously via centralized service
        eventPublisherService.publishEvent(userEventsTopic, String.valueOf(event.getUserId()), event);
        return savedUser;
    }

    // ----------------- LOGIN -----------------
    public Session login(String username, String password, String ipAddress, String userAgent) {
        // Attempt to find user
        User user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null) {
            // Log FAILED (User not found)
            saveAuditLog("LOGIN_FAILED", username, "USER", null, "User not found", 404, ipAddress);
            throw new ResourceNotFoundException("User not found");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            // Log FAILED (Bad credentials)
            saveAuditLog("LOGIN_FAILED", String.valueOf(user.getUserId()), "USER", String.valueOf(user.getUserId()),
                    "Invalid credentials", 401, ipAddress);
            throw new InvalidCredentialsException("Invalid credentials");
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

        Session savedSession = sessionRepository.save(session);
        // Log SUCCESS
        saveAuditLog("LOGIN_SUCCESS", String.valueOf(user.getUserId()), "SESSION",
                String.valueOf(savedSession.getSessionId()), "Login successful", 200, ipAddress);
        return savedSession;
    }

    // ----------------- LOGOUT -----------------
    public void logout(User user) {
        sessionRepository.findByUserAndIsRevokedFalse(user)
                .forEach(s -> {
                    s.setIsRevoked(true);
                    s.setRevokedAt(LocalDateTime.now());
                    sessionRepository.save(s);
                });
        saveAuditLog("LOGOUT", String.valueOf(user.getUserId()), "USER", String.valueOf(user.getUserId()),
                "User logged out", 200, null);
    }

    // ----------------- REFRESH -----------------
    public TokenResponse refresh(String refreshToken) {
        Session session = sessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid refresh token"));

        if (Boolean.TRUE.equals(session.getIsRevoked())) {
            throw new InvalidCredentialsException("Token revoked");
        }

        // Generate a new access token (valid for 15 minutes)
        String newAccessToken = jwtUtil.generateToken(
                session.getUser().getUsername(),
                session.getUser().getRole().name());

        session.setAccessToken(newAccessToken);
        session.setExpiryTime(LocalDateTime.now().plusMinutes(15));
        sessionRepository.save(session);

        return new TokenResponse(
                newAccessToken,
                session.getRefreshToken(),
                "Bearer",
                15 * 60L);
    }

    // ----------------- HELPER -----------------
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    private void saveAuditLog(String eventType, String userId, String entityType, String entityId, String description,
            int statusCode, String ipAddress) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .serviceName("AUTH-SERVICE")
                    .eventType(eventType)
                    .userId(userId)
                    .affectedEntityType(entityType)
                    .affectedEntityId(entityId)
                    .description(description)
                    .statusCode(statusCode)
                    .ipAddress(ipAddress)
                    .build());
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage());
        }
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
