package com.example.auth_service.repository;

import com.example.auth_service.entity.RefreshToken;
import com.example.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);

    List<RefreshToken> findByUser(User user);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);

    long countByExpiresAtBefore(LocalDateTime dateTime);
}
