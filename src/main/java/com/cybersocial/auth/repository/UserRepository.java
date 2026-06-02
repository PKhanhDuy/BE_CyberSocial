package com.cybersocial.auth.repository;

import com.cybersocial.user.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            select u from User u
            where u.id <> :currentUserId
              and (:query = ''
                or lower(u.displayName) like concat('%', lower(:query), '%')
                or lower(u.email) like concat('%', lower(:query), '%'))
            order by u.displayName asc
            """)
    Page<User> searchUsers(
            @Param("currentUserId") UUID currentUserId,
            @Param("query") String query,
            Pageable pageable
    );
}
