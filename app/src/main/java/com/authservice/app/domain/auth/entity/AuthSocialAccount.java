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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auth_social_accounts")
@Getter
@NoArgsConstructor
public class AuthSocialAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID userId;

	@Column(name = "provider", nullable = false)
	private String provider;

	@Column(name = "provider_user_key", nullable = false)
	private String providerUserKey;

	@Column(name = "provider_email")
	private String providerEmail;

	@Column(name = "linked_at", nullable = false)
	private LocalDateTime linkedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public AuthSocialAccount(UUID userId, String provider, String providerUserKey, String providerEmail) {
		this.userId = userId;
		this.provider = provider;
		this.providerUserKey = providerUserKey;
		this.providerEmail = providerEmail;
		this.linkedAt = LocalDateTime.now();
	}

	public void refreshLink(UUID userId, String providerEmail) {
		this.userId = userId;
		this.providerEmail = providerEmail;
		this.linkedAt = LocalDateTime.now();
	}

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		if (linkedAt == null) {
			linkedAt = now;
		}
		createdAt = now;
	}

	@PreUpdate
	void onUpdate() {
		if (linkedAt == null) {
			linkedAt = LocalDateTime.now();
		}
	}
}
