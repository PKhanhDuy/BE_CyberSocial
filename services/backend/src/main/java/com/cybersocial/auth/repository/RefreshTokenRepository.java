package com.cybersocial.auth.repository;

import com.cybersocial.auth.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken token set token.revokedAt = :revokedAt where token.user.id = :userId and token.revokedAt is null")
    void revokeActiveTokensForUser(UUID userId, Instant revokedAt);

    @Modifying
    @Query("delete from RefreshToken token where token.expiresAt < :cutoff")
    void deleteExpiredBefore(Instant cutoff);
}
