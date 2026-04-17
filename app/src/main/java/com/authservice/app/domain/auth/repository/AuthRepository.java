package com.authservice.app.domain.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.authservice.app.domain.auth.entity.Auth;
import com.authservice.app.domain.auth.support.Uuid32;

public interface AuthRepository extends JpaRepository<Auth, String> {
	Optional<Auth> findByUserId(String userId);
	Optional<Auth> findByLoginId(String loginId);

	default Optional<Auth> findByUserId(UUID userId) {
		return findByUserId(Uuid32.fromUuid(userId));
	}

	default Optional<Auth> findByUsername(String username) {
		return findByLoginId(username);
	}
}
