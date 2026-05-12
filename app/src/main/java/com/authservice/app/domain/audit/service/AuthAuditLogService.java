package com.authservice.app.domain.audit.service;

import com.authservice.app.domain.auth.model.AuthChannel;
import com.authservice.app.domain.auth.model.AuthFailureReason;
import com.authservice.app.domain.auth.model.AuthLoginResult;
import com.authservice.app.domain.auth.model.OAuthProvider;
import com.authservice.app.domain.auth.support.AuthPrincipalNames;
import io.github.jho951.platform.governance.api.AuditEntry;
import io.github.jho951.platform.governance.api.GovernanceAuditRecorder;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AuthAuditLogService {

	private static final String UNKNOWN = "unknown";
	private static final String UNKNOWN_LOGIN_ID = "unknown-login-id";
	private static final String INTERNAL_ACTOR_SYSTEM = "gateway-or-user-service";

	private final GovernanceAuditRecorder auditRecorder;

	private void record(String message, Map<String, String> attributes) {
		LinkedHashMap<String, String> details = new LinkedHashMap<>(attributes);
		auditRecorder.record(new AuditEntry(
			"auth",
			message,
			details,
			Instant.now()
		));
	}

	private Map<String, String> loginAuditAttributes(AuthLoginResult result, String loginId, String reasonCode) {
		AuditAttributes attributes = new AuditAttributes()
			.add("eventType", "LOGIN")
			.add("result", result.value())
			.add("actorId", loginIdOrUnknown(loginId))
			.add("resourceType", "AUTH_ACCOUNT")
			.add("resourceId", loginIdOrUnknown(loginId))
			.add("channel", AuthChannel.PASSWORD.value());

		if (result == AuthLoginResult.SUCCESS) {
			attributes.add("actorType", "USER");
		} else {
			attributes
				.add("actorType", "ANONYMOUS")
				.add("reason", reasonOrDefault(reasonCode));
		}
		return attributes.toMap();
	}

	private AuditAttributes sessionAuditAttributes(String eventType, String userId, AuthChannel channel) {
		return new AuditAttributes()
			.add("eventType", eventType)
			.add("result", AuthLoginResult.SUCCESS.value())
			.add("actorType", "USER")
			.add("actorId", stringOrUnknown(userId))
			.add("resourceType", "AUTH_SESSION")
			.add("resourceId", stringOrUnknown(userId))
			.add("channel", channel == null ? "unknown" : channel.value());
	}

	private AuditAttributes internalAccountAttributes(String eventType, String userId) {
		return new AuditAttributes()
			.add("eventType", eventType)
			.add("result", AuthLoginResult.SUCCESS.value())
			.add("actorType", "SERVICE")
			.add("actorId", AuthPrincipalNames.INTERNAL_SERVICE)
			.add("actorSystem", INTERNAL_ACTOR_SYSTEM)
			.add("resourceType", "AUTH_ACCOUNT")
			.add("resourceId", stringOrUnknown(userId));
	}

	private static String loginIdOrUnknown(String loginId) {
		return loginId == null || loginId.isBlank() ? UNKNOWN_LOGIN_ID : loginId;
	}

	private static String stringOrUnknown(String value) {
		return value == null || value.isBlank() ? UNKNOWN : value;
	}

	private static String reasonOrDefault(String reasonCode) {
		return reasonCode == null || reasonCode.isBlank() ? AuthFailureReason.AUTH_FAILURE.code() : reasonCode;
	}

	private static final class AuditAttributes {
		private final LinkedHashMap<String, String> values = new LinkedHashMap<>();

		private AuditAttributes add(String key, String value) {
			values.put(key, value);
			return this;
		}

		private Map<String, String> toMap() {
			return values;
		}
	}

	public AuthAuditLogService(GovernanceAuditRecorder auditRecorder) {
		this.auditRecorder = auditRecorder;
	}

	public void logPasswordLoginSuccess(String loginId) {
		record("AUTH_LOGIN_PASSWORD", loginAuditAttributes(AuthLoginResult.SUCCESS, loginId, null));
	}

	public void logPasswordLoginFailure(String loginId, AuthFailureReason reason) {
		logPasswordLoginFailure(loginId, reason == null ? null : reason.code());
	}

	public void logPasswordLoginFailure(String loginId, String reasonCode) {
		record("AUTH_LOGIN_PASSWORD", loginAuditAttributes(AuthLoginResult.FAILURE, loginId, reasonCode));
	}

	public void logSsoLoginSuccess(String userId, OAuthProvider provider) {
		record("AUTH_LOGIN_SSO", sessionAuditAttributes("LOGIN", userId, AuthChannel.SSO)
			.add("provider", provider == null ? UNKNOWN : provider.externalName())
			.toMap());
	}

	public void logLogout(String userId, AuthChannel channel) {
		record("AUTH_LOGOUT", sessionAuditAttributes("LOGOUT", userId, channel).toMap());
	}

	public void logInternalAccountCreate(String userId, String loginId) {
		record("AUTH_INTERNAL_ACCOUNT_CREATE", internalAccountAttributes("CREATE", userId)
			.add("loginId", loginIdOrUnknown(loginId))
			.toMap());
	}

	public void logInternalAccountDelete(String userId) {
		record("AUTH_INTERNAL_ACCOUNT_DELETE", internalAccountAttributes("DELETE", userId).toMap());
	}


}
