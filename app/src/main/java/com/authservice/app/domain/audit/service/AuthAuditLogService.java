package com.authservice.app.domain.audit.service;

import io.github.jho951.platform.governance.api.AuditEntry;
import io.github.jho951.platform.governance.api.AuditLogRecorder;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AuthAuditLogService {

	private final AuditLogRecorder auditLogRecorder;

	public AuthAuditLogService(AuditLogRecorder auditLogRecorder) {
		this.auditLogRecorder = auditLogRecorder;
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
		auditLogRecorder.record(new AuditEntry(
			"auth",
			message,
			new LinkedHashMap<>(attributes),
			Instant.now()
		));
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
