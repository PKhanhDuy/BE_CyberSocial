package com.cybersocial.admin.audit;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, UUID> {

    Page<AdminActionLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
