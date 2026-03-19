package com.authservice.app.domain.auth.repository;

import com.authservice.app.domain.auth.entity.AuthLoginAttempt;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthLoginAttemptRepository extends JpaRepository<AuthLoginAttempt, UUID> {
}
