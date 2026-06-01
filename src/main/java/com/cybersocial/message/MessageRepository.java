package com.cybersocial.message;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query(
            value = """
            select message from Message message
            join fetch message.sender
            where message.conversation.id = :conversationId
            order by message.createdAt desc
            """,
            countQuery = """
            select count(message) from Message message
            where message.conversation.id = :conversationId
            """
    )
    Page<Message> findByConversationIdWithSenderOrderByCreatedAtDesc(
            @Param("conversationId") UUID conversationId,
            Pageable pageable
    );

    Optional<Message> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);
}
