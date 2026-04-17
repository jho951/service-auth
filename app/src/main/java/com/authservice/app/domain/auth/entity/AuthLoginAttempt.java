package com.authservice.app.domain.auth.entity;

import com.authservice.app.domain.auth.support.Uuid32;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auth_login_attempts")
@Getter
@NoArgsConstructor
public class AuthLoginAttempt {

	@Id
	@Getter(AccessLevel.NONE)
	@Column(name = "id", nullable = false, updatable = false, length = 32, columnDefinition = "char(32)")
	private String id;

	@Column(name = "login_id", nullable = false)
	private String loginId;

	@Column(name = "ip")
	private String ip;

	@Column(name = "result", nullable = false)
	private String result;

	@Column(name = "attempted_at", nullable = false, updatable = false)
	private LocalDateTime attemptedAt;

	public AuthLoginAttempt(String loginId, String ip, String result) {
		this.loginId = loginId;
		this.ip = ip;
		this.result = result;
	}

	public UUID getId() {
		return Uuid32.toUuid(id);
	}

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = Uuid32.generate();
		}
		attemptedAt = LocalDateTime.now();
	}
}
