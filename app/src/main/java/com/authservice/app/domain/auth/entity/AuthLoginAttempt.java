package com.authservice.app.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auth_login_attempts")
@Getter
@NoArgsConstructor
public class AuthLoginAttempt {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

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

	@PrePersist
	void onCreate() {
		attemptedAt = LocalDateTime.now();
	}
}
