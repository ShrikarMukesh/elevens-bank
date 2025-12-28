package com.auth.service;

import com.auth.dto.TokenResponse;
import com.auth.entity.Session;
import com.auth.entity.User;
import com.auth.kafka.EventPublisherService;
import com.auth.kafka.UserCreatedEvent;
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
    /**
     * Registers a new user.
     *
     * <h2>Interview Topic: Password Storage</h2>
     * <p>
     * <b>Q: How should you store passwords?</b><br>
     * A: Never store them in plain text. We use <b>BCrypt</b> (Salted Hashing).
     * <br>
     * <b>Q: Why not MD5 or SHA-256?</b><br>
     * A: They are too fast, making them vulnerable to Brute Force/Rainbow Table
     * attacks. BCrypt is "slow by design" (Work Factor).
     * </p>
     *
     * <h2>Interview Topic: Event Driven Architecture</h2>
     * <p>
     * <b>Q: What happens after registration?</b><br>
     * A: We don't just save to DB. We publish a {@code UserCreatedEvent} to Kafka.
     * This allows other services (Notification, Account) to react asynchronously
     * without tighter coupling (Decoupling).
     * </p>
     */
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
        UserCreatedEvent event = UserCreatedEvent.builder()
                .userId(String.valueOf(user.getUserId()))
                .username(user.getUsername())
                .fullName(user.getUsername()) // or concatenate if you store first+last in future
                .email(user.getEmail())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .build();

        // Publish asynchronously via centralized service
        eventPublisherService.publishEvent("bank.user.event.v1", String.valueOf(event.getUserId()), event);
        return savedUser;
    }

    // ----------------- LOGIN -----------------
    /**
     * Authenticates a user and issues tokens.
     *
     * <h2>Interview Topic: JWT (JSON Web Token)</h2>
     * <p>
     * <b>Q: What is the structure of a JWT?</b><br>
     * A: Header (Algorithm), Payload (Claims: sub, role, exp), Signature (hashed
     * with Secret Key).
     * </p>
     * <p>
     * <b>Q: Explain Access Token vs Refresh Token?</b><br>
     * A:
     * <ul>
     * <li><b>Access Token:</b> Short-lived (15 mins). Used to access resources.
     * Stateless.</li>
     * <li><b>Refresh Token:</b> Long-lived (7 days). Stored in DB. Used to get new
     * Access Tokens. Stateful (allows Revocation).</li>
     * </ul>
     * This "Dual Token Strategy" balances security (short access window) with user
     * experience (stay logged in).
     * </p>
     */
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
    /**
     * Refreshes the Access Token.
     *
     * <h2>Interview Topic: Token Rotation</h2>
     * <p>
     * <b>Q: Why do we rotate tokens?</b><br>
     * A: If a Refresh Token is stolen, the attacker can use it indefinitely.
     * By rotating (issuing a NEW Access Token + potentially a NEW Refresh Token)
     * and tracking usage, we can detect theft.
     * (Currently, this implementation only issues new Access Tokens, which is a
     * Sliding Session strategy).
     * </p>
     */
    public TokenResponse refresh(String refreshToken) {
        Session session = sessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (Boolean.TRUE.equals(session.getIsRevoked())) {
            throw new RuntimeException("Token revoked");
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
