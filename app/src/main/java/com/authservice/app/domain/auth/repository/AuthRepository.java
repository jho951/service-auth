package com.authservice.app.domain.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.authservice.app.domain.auth.entity.Auth;

public interface AuthRepository extends JpaRepository<Auth, UUID> {
	Optional<Auth> findByUserId(UUID userId);
	Optional<Auth> findByLoginId(String loginId);

	default Optional<Auth> findByUsername(String username) {
		return findByLoginId(username);
	}
}
