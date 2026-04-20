package com.authservice.app.domain.auth.entity;

import com.authservice.common.base.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "auth_accounts")
@Getter
@NoArgsConstructor
public class Auth extends BaseEntity {

	@Column(name = "user_id", nullable = false, unique = true, length = 36, columnDefinition = "char(36)")
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID userId;

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
		this.userId = userId;
		this.loginId = loginId;
		this.passwordHash = passwordHash;
		this.accountLocked = false;
		this.failedLoginCount = 0;
		this.passwordUpdatedAt = LocalDateTime.now();
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
		if (passwordUpdatedAt == null) {
			passwordUpdatedAt = LocalDateTime.now();
		}
	}
}
