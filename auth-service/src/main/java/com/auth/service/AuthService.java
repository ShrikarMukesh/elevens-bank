package com.auth.service;

import com.auth.entity.Session;
import com.auth.entity.User;
import com.auth.repository.SessionRepository;
import com.auth.repository.UserRepository;
import com.auth.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService implements UserDetailsService{

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User register(User user) {
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        return userRepository.save(user);
    }

    public Session login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        String accessToken = jwtUtil.generateToken(username);
        String refreshToken = UUID.randomUUID().toString();

        Session session = Session.builder()
                .user(user)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiryTime(LocalDateTime.now().plusMinutes(15))
                .build();

        return sessionRepository.save(session);
    }

    public void logout(User user) {
        sessionRepository.deleteByUser(user);
    }

    public Session refresh(String refreshToken) {
        Session session = sessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (session.getIsRevoked()) throw new RuntimeException("Token revoked");

        String newAccessToken = jwtUtil.generateToken(session.getUser().getUsername());
        session.setAccessToken(newAccessToken);
        session.setExpiryTime(LocalDateTime.now().plusMinutes(15));

        return sessionRepository.save(session);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        com.auth.entity.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }

}
