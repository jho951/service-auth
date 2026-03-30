package com.authservice.app.domain.audit.service;

import com.auditlog.api.AuditActorType;
import com.auditlog.api.AuditEvent;
import com.auditlog.api.AuditEventType;
import com.auditlog.api.AuditLogger;
import org.springframework.stereotype.Service;

@Service
public class AuthAuditLogService {

	private final AuditLogger auditLogger;

	public AuthAuditLogService(AuditLogger auditLogger) {
		this.auditLogger = auditLogger;
	}

	public void logPasswordLoginSuccess(String loginId) {
		auditLogger.logSuccess(AuditEvent.builder(AuditEventType.LOGIN, "AUTH_LOGIN_PASSWORD")
			.actor(loginIdOrUnknown(loginId), AuditActorType.USER, null)
			.resource("AUTH_ACCOUNT", loginIdOrUnknown(loginId))
			.detail("channel", "PASSWORD"));
	}

	public void logPasswordLoginFailure(String loginId, String reasonCode) {
		auditLogger.logFailure(AuditEvent.builder(AuditEventType.LOGIN, "AUTH_LOGIN_PASSWORD")
			.actor(loginIdOrUnknown(loginId), AuditActorType.ANONYMOUS, null)
			.resource("AUTH_ACCOUNT", loginIdOrUnknown(loginId))
			.detail("channel", "PASSWORD"), reasonOrDefault(reasonCode));
	}

	public void logSsoLoginSuccess(String userId, String provider) {
		auditLogger.logSuccess(AuditEvent.builder(AuditEventType.LOGIN, "AUTH_LOGIN_SSO")
			.actor(stringOrUnknown(userId), AuditActorType.USER, null)
			.resource("AUTH_SESSION", stringOrUnknown(userId))
			.detail("channel", "SSO")
			.detail("provider", stringOrUnknown(provider)));
	}

	public void logLogout(String userId, String channel) {
		auditLogger.logSuccess(AuditEvent.builder(AuditEventType.LOGOUT, "AUTH_LOGOUT")
			.actor(stringOrUnknown(userId), AuditActorType.USER, null)
			.resource("AUTH_SESSION", stringOrUnknown(userId))
			.detail("channel", stringOrUnknown(channel)));
	}

	public void logInternalAccountCreate(String userId, String loginId) {
		auditLogger.logSuccess(AuditEvent.builder(AuditEventType.CREATE, "AUTH_INTERNAL_ACCOUNT_CREATE")
			.actor("internal-service", AuditActorType.SERVICE, "gateway-or-user-service")
			.resource("AUTH_ACCOUNT", stringOrUnknown(userId))
			.detail("loginId", loginIdOrUnknown(loginId)));
	}

	public void logInternalAccountDelete(String userId) {
		auditLogger.logSuccess(AuditEvent.builder(AuditEventType.DELETE, "AUTH_INTERNAL_ACCOUNT_DELETE")
			.actor("internal-service", AuditActorType.SERVICE, "gateway-or-user-service")
			.resource("AUTH_ACCOUNT", stringOrUnknown(userId)));
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
