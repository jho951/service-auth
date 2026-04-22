package com.authservice.app.domain.audit.service;

import com.auditlog.api.AuditActorType;
import com.auditlog.api.AuditEvent;
import com.auditlog.api.AuditEventType;
import com.auditlog.api.AuditLogger;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AuthAuditLogService {

	private final AuditLogger auditLogger;

	public AuthAuditLogService(AuditLogger auditLogger) {
		this.auditLogger = auditLogger;
	}

	public void logPasswordLoginSuccess(String loginId) {
		record("AUTH_LOGIN_PASSWORD", Map.of(
			"eventType", "LOGIN",
			"result", "SUCCESS",
			"actorType", "USER",
			"actorId", loginIdOrUnknown(loginId),
			"resourceType", "AUTH_ACCOUNT",
			"resourceId", loginIdOrUnknown(loginId),
			"channel", "PASSWORD"
		));
	}

	public void logPasswordLoginFailure(String loginId, String reasonCode) {
		record("AUTH_LOGIN_PASSWORD", Map.of(
			"eventType", "LOGIN",
			"result", "FAILURE",
			"reason", reasonOrDefault(reasonCode),
			"actorType", "ANONYMOUS",
			"actorId", loginIdOrUnknown(loginId),
			"resourceType", "AUTH_ACCOUNT",
			"resourceId", loginIdOrUnknown(loginId),
			"channel", "PASSWORD"
		));
	}

	public void logSsoLoginSuccess(String userId, String provider) {
		record("AUTH_LOGIN_SSO", Map.of(
			"eventType", "LOGIN",
			"result", "SUCCESS",
			"actorType", "USER",
			"actorId", stringOrUnknown(userId),
			"resourceType", "AUTH_SESSION",
			"resourceId", stringOrUnknown(userId),
			"channel", "SSO",
			"provider", stringOrUnknown(provider)
		));
	}

	public void logLogout(String userId, String channel) {
		record("AUTH_LOGOUT", Map.of(
			"eventType", "LOGOUT",
			"result", "SUCCESS",
			"actorType", "USER",
			"actorId", stringOrUnknown(userId),
			"resourceType", "AUTH_SESSION",
			"resourceId", stringOrUnknown(userId),
			"channel", stringOrUnknown(channel)
		));
	}

	public void logInternalAccountCreate(String userId, String loginId) {
		record("AUTH_INTERNAL_ACCOUNT_CREATE", Map.of(
			"eventType", "CREATE",
			"result", "SUCCESS",
			"actorType", "SERVICE",
			"actorId", "internal-service",
			"actorSystem", "gateway-or-user-service",
			"resourceType", "AUTH_ACCOUNT",
			"resourceId", stringOrUnknown(userId),
			"loginId", loginIdOrUnknown(loginId)
		));
	}

	public void logInternalAccountDelete(String userId) {
		record("AUTH_INTERNAL_ACCOUNT_DELETE", Map.of(
			"eventType", "DELETE",
			"result", "SUCCESS",
			"actorType", "SERVICE",
			"actorId", "internal-service",
			"actorSystem", "gateway-or-user-service",
			"resourceType", "AUTH_ACCOUNT",
			"resourceId", stringOrUnknown(userId)
		));
	}

	private void record(String message, Map<String, String> attributes) {
		LinkedHashMap<String, Object> details = new LinkedHashMap<>(attributes);
		auditLogger.log(
			AuditEvent.builder(resolveEventType(attributes.get("eventType")), message)
				.occurredAt(Instant.now())
				.actor(
					stringOrUnknown(attributes.get("actorId")),
					resolveActorType(attributes.get("actorType")),
					stringOrUnknown(attributes.get("actorId"))
				)
				.resource(
					stringOrUnknown(attributes.get("resourceType")),
					stringOrUnknown(attributes.get("resourceId"))
				)
				.reason(reasonOrDefault(attributes.get("reason")))
				.details(details)
				.result("FAILURE".equalsIgnoreCase(attributes.get("result"))
					? com.auditlog.api.AuditResult.FAILURE
					: com.auditlog.api.AuditResult.SUCCESS)
				.build()
		);
	}

	private static AuditEventType resolveEventType(String eventType) {
		if (eventType == null || eventType.isBlank()) {
			return AuditEventType.CUSTOM;
		}
		try {
			return AuditEventType.valueOf(eventType.trim().toUpperCase());
		} catch (IllegalArgumentException ignored) {
			return AuditEventType.CUSTOM;
		}
	}

	private static AuditActorType resolveActorType(String actorType) {
		if (actorType == null || actorType.isBlank()) {
			return AuditActorType.UNKNOWN;
		}
		try {
			return AuditActorType.valueOf(actorType.trim().toUpperCase());
		} catch (IllegalArgumentException ignored) {
			return AuditActorType.UNKNOWN;
		}
	}

	private static String loginIdOrUnknown(String loginId) {
		return loginId == null || loginId.isBlank() ? "unknown-login-id" : loginId;
	}

	private static String stringOrUnknown(String value) {
		return value == null || value.isBlank() ? "unknown" : value;
	}

	private static String reasonOrDefault(String reasonCode) {
		return reasonCode == null || reasonCode.isBlank() ? "AUTH_FAILURE" : reasonCode;
	}
}
