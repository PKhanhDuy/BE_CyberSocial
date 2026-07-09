package com.cybersocial.message;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {

    Optional<MessageReaction> findByMessageIdAndUserId(UUID messageId, UUID userId);

    @Query("""
            select reaction from MessageReaction reaction
            join fetch reaction.user
            where reaction.message.id = :messageId
            order by reaction.createdAt asc
            """)
    List<MessageReaction> findByMessageIdWithUserOrderByCreatedAtAsc(@Param("messageId") UUID messageId);

    @Query("""
            select reaction from MessageReaction reaction
            join fetch reaction.user
            where reaction.message.id in :messageIds
            order by reaction.createdAt asc
            """)
    List<MessageReaction> findByMessageIdsWithUserOrderByCreatedAtAsc(
            @Param("messageIds") Collection<UUID> messageIds
    );
}
