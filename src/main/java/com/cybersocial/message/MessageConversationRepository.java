package com.cybersocial.message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageConversationRepository extends JpaRepository<MessageConversation, UUID> {

    @Query("""
            select conversation from MessageConversation conversation
            join fetch conversation.userOne
            join fetch conversation.userTwo
            where conversation.userOne.id = :userId
               or conversation.userTwo.id = :userId
            order by conversation.updatedAt desc
            """)
    List<MessageConversation> findByParticipantOrderByUpdatedAtDesc(@Param("userId") UUID userId);

    @Query("""
            select conversation from MessageConversation conversation
            join fetch conversation.userOne
            join fetch conversation.userTwo
            where conversation.userOne.id = :userOneId
              and conversation.userTwo.id = :userTwoId
            """)
    Optional<MessageConversation> findByOrderedParticipants(
            @Param("userOneId") UUID userOneId,
            @Param("userTwoId") UUID userTwoId
    );
}
