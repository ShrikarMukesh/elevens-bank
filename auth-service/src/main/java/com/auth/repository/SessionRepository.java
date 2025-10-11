package com.auth.repository;

import com.auth.entity.Session;
import com.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {
    Optional<Session> findByAccessToken(String accessToken);
    Optional<Session> findByRefreshToken(String refreshToken);
    List<Session> findByUserAndIsRevokedFalse(User user);
    void deleteByUser(User user);
}
