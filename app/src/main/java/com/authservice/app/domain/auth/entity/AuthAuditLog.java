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
@Table(name = "auth_audit_logs")
@Getter
@NoArgsConstructor
public class AuthAuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@Column(name = "user_id", columnDefinition = "BINARY(16)")
	private UUID userId;

	@Column(name = "auth_account_id", columnDefinition = "BINARY(16)")
	private UUID authAccountId;

	@Column(name = "event_type", nullable = false)
	private String eventType;

	@Column(name = "ip")
	private String ip;

	@Column(name = "user_agent")
	private String userAgent;

	@Column(name = "result", nullable = false)
	private String result;

	@Column(name = "metadata_json", length = 4000)
	private String metadataJson;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public AuthAuditLog(
		UUID userId,
		UUID authAccountId,
		String eventType,
		String ip,
		String userAgent,
		String result,
		String metadataJson
	) {
		this.userId = userId;
		this.authAccountId = authAccountId;
		this.eventType = eventType;
		this.ip = ip;
		this.userAgent = userAgent;
		this.result = result;
		this.metadataJson = metadataJson;
	}

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
