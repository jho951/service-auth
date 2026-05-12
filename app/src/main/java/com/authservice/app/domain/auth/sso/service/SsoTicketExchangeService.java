package com.authservice.app.domain.auth.sso.service;

import com.authservice.app.domain.auth.config.AuthHttpProperties;
import com.authservice.app.domain.auth.model.AuthPrincipal;
import com.authservice.app.domain.auth.model.AuthTokens;
import com.authservice.app.domain.auth.model.OAuthProvider;
import com.authservice.app.domain.auth.sso.config.SsoProperties;
import com.authservice.app.domain.auth.sso.model.SsoPageType;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoSessionPayload;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoTicketPayload;
import com.authservice.app.domain.auth.support.AuthCookieResponseWriter;
import com.authservice.app.domain.audit.service.AuthAuditLogService;
import com.authservice.app.domain.auth.service.AuthRedisRefreshTokenStore;
import com.authservice.app.security.platform.issuer.PlatformAuthTokenIssuer;
import com.authservice.common.base.constant.ErrorCode;
import com.authservice.common.base.exception.GlobalException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class SsoTicketExchangeService {

	private final SsoProperties properties;
	private final SsoSessionStore sessionStore;
	private final PlatformAuthTokenIssuer platformAuthTokenIssuer;
	private final AuthRedisRefreshTokenStore refreshTokenStore;
	private final SsoCookieService cookieService;
	private final AuthCookieResponseWriter authCookieResponseWriter;
	private final AdminIpGuardService adminIpGuardService;
	private final AuthAuditLogService authAuditLogService;
	private final long refreshTokenTtlSeconds;

	public SsoTicketExchangeService(
		SsoProperties properties,
		SsoSessionStore sessionStore,
		PlatformAuthTokenIssuer platformAuthTokenIssuer,
		AuthRedisRefreshTokenStore refreshTokenStore,
		SsoCookieService cookieService,
		AuthCookieResponseWriter authCookieResponseWriter,
		AdminIpGuardService adminIpGuardService,
		AuthAuditLogService authAuditLogService,
		AuthHttpProperties authHttpProperties
	) {
		this.properties = properties;
		this.sessionStore = sessionStore;
		this.platformAuthTokenIssuer = platformAuthTokenIssuer;
		this.refreshTokenStore = refreshTokenStore;
		this.cookieService = cookieService;
		this.authCookieResponseWriter = authCookieResponseWriter;
		this.adminIpGuardService = adminIpGuardService;
		this.authAuditLogService = authAuditLogService;
		this.refreshTokenTtlSeconds = Math.max(1L, authHttpProperties.getJwt().getRefreshSeconds());
	}

	public ResponseEntity<Void> exchangeTicket(String ticket, HttpServletRequest request) {
		SsoTicketPayload payload = sessionStore.consumeTicket(ticket)
			.orElseThrow(() -> new GlobalException(ErrorCode.UNAUTHORIZED));

		if (SsoPageType.from(payload.getPageType()) == SsoPageType.ADMIN) {
			adminIpGuardService.validate(request);
		}

		String sessionId = UUID.randomUUID().toString();
		Instant sessionExpiresAt = Instant.now().plusSeconds(properties.getSession().getTtlSeconds());
		sessionStore.saveSession(sessionId, toSessionPayload(payload, sessionExpiresAt), sessionExpiresAt);
		authAuditLogService.logSsoLoginSuccess(payload.getUserId(), OAuthProvider.GITHUB);

		AuthPrincipal principal = toPrincipal(payload);
		AuthTokens tokens = platformAuthTokenIssuer.issue(principal);
		refreshTokenStore.save(principal.userId(), tokens.refreshToken(), Instant.now().plusSeconds(refreshTokenTtlSeconds));

		ResponseEntity<Void> response = authCookieResponseWriter.writeTokenCookies(tokens, ResponseEntity.noContent().build());
		return authCookieResponseWriter.appendCookies(response, cookieService.buildSessionCookie(sessionId));
	}

	private SsoSessionPayload toSessionPayload(SsoTicketPayload payload, Instant expiresAt) {
		return new SsoSessionPayload(
			payload.getUserId(),
			payload.getEmail(),
			payload.getName(),
			payload.getAvatarUrl(),
			payload.getRoles(),
			payload.getStatus(),
			expiresAt
		);
	}

	private AuthPrincipal toPrincipal(SsoTicketPayload payload) {
		return new AuthPrincipal(
			payload.getUserId(),
			payload.getRoles() == null ? List.of() : payload.getRoles(),
			Map.of(
				"email", payload.getEmail() == null ? "" : payload.getEmail(),
				"name", payload.getName() == null ? "" : payload.getName(),
				"avatarUrl", payload.getAvatarUrl() == null ? "" : payload.getAvatarUrl(),
				"status", payload.getStatus() == null ? "" : payload.getStatus()
			)
		);
	}
}
