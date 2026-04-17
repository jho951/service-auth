package com.authservice.app.domain.auth.entity;

import com.authservice.app.common.base.entity.BaseEntity;
import com.authservice.app.domain.auth.support.Uuid32;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auth_accounts")
@AttributeOverride(name = "modifiedDate", column = @Column(name = "updated_at", nullable = false))
@Getter
@NoArgsConstructor
public class Auth extends BaseEntity {

	@Id
	@Getter(AccessLevel.NONE)
	@Column(name = "id", nullable = false, updatable = false, length = 32, columnDefinition = "char(32)")
	private String id;

	@Getter(AccessLevel.NONE)
	@Column(name = "user_id", nullable = false, unique = true, length = 32, columnDefinition = "char(32)")
	private String userId;

	@Column(name = "login_id", nullable = false, unique = true)
	private String loginId;

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

	@Builder
	private Auth(UUID userId, String loginId, String passwordHash) {
		this.userId = Uuid32.fromUuid(userId);
		this.loginId = loginId;
		this.passwordHash = passwordHash;
		this.accountLocked = false;
		this.failedLoginCount = 0;
		this.passwordUpdatedAt = LocalDateTime.now();
	}

	public UUID getId() {
		return Uuid32.toUuid(id);
	}

	public UUID getUserId() {
		return Uuid32.toUuid(userId);
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
		if (id == null) {
			id = Uuid32.generate();
		}
		if (passwordUpdatedAt == null) {
			passwordUpdatedAt = LocalDateTime.now();
		}
	}
}
