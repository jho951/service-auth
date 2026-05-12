package com.authservice.app.domain.auth.sso.service;

import com.authservice.app.domain.auth.service.AuthLoginService;
import com.authservice.app.domain.auth.model.AuthChannel;
import com.authservice.app.domain.auth.support.AuthCookieResponseWriter;
import com.authservice.app.domain.auth.support.RefreshTokenExtractor;
import com.authservice.app.domain.audit.service.AuthAuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SsoLogoutService {

	private static final Logger log = LoggerFactory.getLogger(SsoLogoutService.class);

	private final SsoSessionStore sessionStore;
	private final SsoCookieService cookieService;
	private final RefreshTokenExtractor refreshTokenExtractor;
	private final AuthLoginService authLoginService;
	private final AuthAuditLogService authAuditLogService;
	private final AuthCookieResponseWriter authCookieResponseWriter;

	public SsoLogoutService(
		SsoSessionStore sessionStore,
		SsoCookieService cookieService,
		RefreshTokenExtractor refreshTokenExtractor,
		AuthLoginService authLoginService,
		AuthAuditLogService authAuditLogService,
		AuthCookieResponseWriter authCookieResponseWriter
	) {
		this.sessionStore = sessionStore;
		this.cookieService = cookieService;
		this.refreshTokenExtractor = refreshTokenExtractor;
		this.authLoginService = authLoginService;
		this.authAuditLogService = authAuditLogService;
		this.authCookieResponseWriter = authCookieResponseWriter;
	}

	public ResponseEntity<Void> logout(HttpServletRequest request) {
		String actorId = cookieService.extractSessionId(request)
			.flatMap(sessionStore::findSession)
			.map(session -> session.getUserId())
			.filter(userId -> userId != null && !userId.isBlank())
			.orElse("unknown");

		cookieService.extractSessionId(request).ifPresent(sessionStore::revokeSession);
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated() && authentication.getName() != null && !authentication.getName().isBlank()) {
			actorId = authentication.getName();
		}

		refreshTokenExtractor.extractOptional(request).ifPresent(refreshToken -> {
			try {
				authLoginService.logout(refreshToken);
			} catch (RuntimeException ex) {
				log.warn("Refresh token revoke skipped during logout. reason={}", ex.getClass().getSimpleName());
			}
		});

		authAuditLogService.logLogout(actorId, AuthChannel.SSO);
		ResponseEntity<Void> response = ResponseEntity.noContent().build();
		response = authCookieResponseWriter.appendCookies(response, cookieService.clearSessionCookieValue());
		return authCookieResponseWriter.clearTokenCookies(response);
	}
}
