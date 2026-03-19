package com.authservice.app.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auth_accounts")
@Getter
@NoArgsConstructor
public class Auth {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@Column(name = "user_id", nullable = false, unique = true, columnDefinition = "BINARY(16)")
	private UUID userId;

	@Column(name = "login_id", nullable = false, unique = true)
	private String loginId;

	@Column(name = "email", nullable = false)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "account_locked", nullable = false)
	private boolean accountLocked;

	@Column(name = "failed_login_count", nullable = false)
	private int failedLoginCount;

	@Column(name = "password_updated_at", nullable = false)
	private LocalDateTime passwordUpdatedAt;

	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Builder
	private Auth(UUID userId, String loginId, String email, String passwordHash) {
		this.userId = userId;
		this.loginId = loginId;
		this.email = email;
		this.passwordHash = passwordHash;
		this.accountLocked = false;
		this.failedLoginCount = 0;
		LocalDateTime now = LocalDateTime.now();
		this.passwordUpdatedAt = now;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public String getUsername() {
		return loginId;
	}

	public void markLoginSuccess() {
		this.failedLoginCount = 0;
		this.accountLocked = false;
		this.lastLoginAt = LocalDateTime.now();
	}

	public void markLoginFailure() {
		this.failedLoginCount += 1;
	}

	public void lockAccount() {
		this.accountLocked = true;
	}

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		if (passwordUpdatedAt == null) {
			passwordUpdatedAt = now;
		}
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
