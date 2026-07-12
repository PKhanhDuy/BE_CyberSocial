package com.cybersocial.friend;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    @Query("""
            select f from Friendship f
            join fetch f.requester
            join fetch f.addressee
            where (f.requester.id = :userId or f.addressee.id = :userId)
              and f.status = :status
            order by f.updatedAt desc
            """)
    List<Friendship> findByParticipantAndStatus(
            @Param("userId") UUID userId,
            @Param("status") FriendshipStatus status
    );

    @Query("""
            select f from Friendship f
            join fetch f.requester
            join fetch f.addressee
            where f.addressee.id = :userId
              and f.status = com.cybersocial.friend.FriendshipStatus.PENDING
            order by f.createdAt desc
            """)
    List<Friendship> findIncomingPending(@Param("userId") UUID userId);

    @Query("""
            select f from Friendship f
            join fetch f.requester
            join fetch f.addressee
            where f.requester.id = :userId
              and f.status = com.cybersocial.friend.FriendshipStatus.PENDING
            order by f.createdAt desc
            """)
    List<Friendship> findOutgoingPending(@Param("userId") UUID userId);

    @Query("""
            select f from Friendship f
            join fetch f.requester
            join fetch f.addressee
            where (f.requester.id = :firstUserId and f.addressee.id = :secondUserId)
               or (f.requester.id = :secondUserId and f.addressee.id = :firstUserId)
            """)
    Optional<Friendship> findBetween(
            @Param("firstUserId") UUID firstUserId,
            @Param("secondUserId") UUID secondUserId
    );

    long countByStatus(FriendshipStatus status);
}
