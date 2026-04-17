package com.authservice.app.domain.auth.entity;

import com.authservice.app.domain.auth.support.Uuid32;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mfa_factors")
@Getter
@NoArgsConstructor
public class MfaFactor {

	@Id
	@Getter(AccessLevel.NONE)
	@Column(name = "id", nullable = false, updatable = false, length = 32, columnDefinition = "char(32)")
	private String id;

	@Getter(AccessLevel.NONE)
	@Column(name = "user_id", nullable = false, length = 32, columnDefinition = "char(32)")
	private String userId;

	@Column(name = "factor_type", nullable = false)
	private String factorType;

	@Column(name = "secret_ref", nullable = false)
	private String secretRef;

	@Column(name = "enabled", nullable = false)
	private boolean enabled;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	public UUID getId() {
		return Uuid32.toUuid(id);
	}

	public UUID getUserId() {
		return Uuid32.toUuid(userId);
	}

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = Uuid32.generate();
		}
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
