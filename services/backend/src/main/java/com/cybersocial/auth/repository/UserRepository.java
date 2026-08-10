package com.cybersocial.auth.repository;

import com.cybersocial.user.User;
import com.cybersocial.user.UserRole;
import java.util.List;
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

    long countByDemoUserTrue();

    List<User> findByDemoUserTrueOrderByEmailAsc();

    @Query("""
            select u from User u
            where u.id <> :currentUserId
              and u.enabled = true
              and (:query = ''
                or lower(u.displayName) like concat('%', lower(:query), '%'))
            order by u.displayName asc
            """)
    Page<User> searchUsers(
            @Param("currentUserId") UUID currentUserId,
            @Param("query") String query,
            Pageable pageable
    );

    long countByEnabledTrue();

    long countByEnabledFalse();

    @Query("""
            select u from User u
            where (:query = ''
                or lower(u.displayName) like concat('%', lower(:query), '%')
                or lower(u.email) like concat('%', lower(:query), '%'))
              and (:enabled is null or u.enabled = :enabled)
              and (:role is null or u.role = :role)
            order by u.createdAt desc
            """)
    Page<User> findForAdmin(
            @Param("query") String query,
            @Param("enabled") Boolean enabled,
            @Param("role") UserRole role,
            Pageable pageable
    );
}
