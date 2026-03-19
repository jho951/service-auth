package com.authservice.app.domain.auth.repository;

import com.authservice.app.domain.auth.entity.AuthAuditLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, UUID> {
}
