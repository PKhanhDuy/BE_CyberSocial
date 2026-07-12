package com.cybersocial.auth.repository;

import com.cybersocial.auth.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update PasswordResetToken token
            set token.usedAt = :usedAt
            where token.user.id = :userId and token.usedAt is null
            """)
    void invalidateActiveTokensForUser(UUID userId, Instant usedAt);

    @Modifying
    @Query("delete from PasswordResetToken token where token.expiresAt < :cutoff")
    void deleteExpiredBefore(Instant cutoff);
}
