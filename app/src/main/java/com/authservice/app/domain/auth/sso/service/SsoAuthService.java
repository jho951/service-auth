package com.authservice.app.domain.auth.sso.service;

import com.auth.config.AuthProperties;
import com.authservice.app.domain.auth.sso.config.SsoProperties;
import com.authservice.app.domain.auth.sso.model.GithubUserProfile;
import com.authservice.app.domain.auth.sso.model.SsoPageType;
import com.authservice.app.domain.auth.sso.model.SsoPrincipal;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoSessionPayload;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoStatePayload;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoTicketPayload;
import com.authservice.app.domain.auth.sso.model.SsoTargetPage;
import com.authservice.app.common.base.constant.ErrorCode;
import com.authservice.app.common.base.exception.GlobalException;
import com.authservice.app.domain.auth.service.AuthAuditService;
import com.authservice.app.domain.auth.service.AuthRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class SsoAuthService {

	private static final Logger log = LoggerFactory.getLogger(SsoAuthService.class);

	private final SsoProperties properties;
	private final AuthProperties authProperties;
	private final SsoSessionStore sessionStore;
	private final SsoUserService ssoUserService;
	private final SsoCookieService cookieService;
	private final AdminIpGuardService adminIpGuardService;
	private final AuthAuditService authAuditService;

	public SsoAuthService(
		SsoProperties properties,
		AuthProperties authProperties,
		SsoSessionStore sessionStore,
		SsoUserService ssoUserService,
		SsoCookieService cookieService,
		AdminIpGuardService adminIpGuardService,
		AuthAuditService authAuditService
	) {
		this.properties = properties;
		this.authProperties = authProperties;
		this.sessionStore = sessionStore;
		this.ssoUserService = ssoUserService;
		this.cookieService = cookieService;
		this.adminIpGuardService = adminIpGuardService;
		this.authAuditService = authAuditService;
	}

	public org.springframework.http.ResponseEntity<Void> startGithubLogin(String page, String redirectUri, HttpServletRequest request) {
		SsoTargetPage targetPage = resolveTargetPage(page, redirectUri);
		if (targetPage.pageType() == SsoPageType.ADMIN) {
			adminIpGuardService.validate(request);
		}

		String state = UUID.randomUUID().toString();
		Instant expiresAt = Instant.now().plusSeconds(properties.getStateTtlSeconds());
		sessionStore.saveState(
			state,
			new SsoStatePayload(targetPage.redirectUri(), targetPage.pageType().name(), expiresAt),
			expiresAt
		);

		String authorizationUri = UriComponentsBuilder
			.fromPath(authProperties.getOauth2().getAuthorizationBaseUri())
			.pathSegment("github")
			.build()
			.toUriString();

		return org.springframework.http.ResponseEntity.status(302)
			.header(org.springframework.http.HttpHeaders.SET_COOKIE, cookieService.buildOAuthStateCookie(state, properties.getStateTtlSeconds()))
			.location(URI.create(authorizationUri))
			.build();
	}

	public org.springframework.http.ResponseEntity<Void> startOAuthLogin(
		String provider,
		String page,
		String redirectUri,
		HttpServletRequest request
	) {
		if (!"github".equalsIgnoreCase(provider)) {
			throw new GlobalException(ErrorCode.INVALID_REQUEST);
		}
		return startGithubLogin(page, redirectUri, request);
	}

	public org.springframework.http.ResponseEntity<Void> exchangeTicket(String ticket, HttpServletRequest request) {
		SsoTicketPayload payload = sessionStore.consumeTicket(ticket)
			.orElseThrow(() -> new GlobalException(ErrorCode.UNAUTHORIZED));

		if (SsoPageType.from(payload.getPageType()) == SsoPageType.ADMIN) {
			adminIpGuardService.validate(request);
		}

		String sessionId = UUID.randomUUID().toString();
		Instant expiresAt = Instant.now().plusSeconds(properties.getSession().getTtlSeconds());
		sessionStore.saveSession(
			sessionId,
			new SsoSessionPayload(
				payload.getUserId(),
				payload.getEmail(),
				payload.getName(),
				payload.getAvatarUrl(),
				payload.getRoles(),
				expiresAt
			),
			expiresAt
		);
		authAuditService.log("SSO_LOGIN_SUCCESS", "SUCCESS", UUID.fromString(payload.getUserId()), AuthRequestContext.from(request), null);

		return cookieService.writeSessionCookie(sessionId);
	}

	public org.springframework.http.ResponseEntity<com.authservice.app.domain.auth.sso.dto.SsoResponse.InternalSessionValidationResponse> validateInternalSession(
		HttpServletRequest request
	) {
		String sessionId = cookieService.extractSessionId(request).orElse(null);
		if (sessionId == null || sessionId.isBlank()) {
			return org.springframework.http.ResponseEntity.status(401)
				.body(new com.authservice.app.domain.auth.sso.dto.SsoResponse.InternalSessionValidationResponse(false, "", "", ""));
		}

		return sessionStore.findSession(sessionId)
			.map(payload -> org.springframework.http.ResponseEntity.ok(
				new com.authservice.app.domain.auth.sso.dto.SsoResponse.InternalSessionValidationResponse(
					true,
					payload.getUserId(),
					resolvePrimaryRole(payload),
					sessionId
				)
			))
			.orElseGet(() -> org.springframework.http.ResponseEntity.status(401)
				.body(new com.authservice.app.domain.auth.sso.dto.SsoResponse.InternalSessionValidationResponse(false, "", "", "")));
	}

	public URI completeOAuthLogin(SsoPrincipal principal, HttpServletRequest request) {
		SsoStatePayload statePayload = consumeOAuthState(request);

		String ticket = UUID.randomUUID().toString();
		Instant expiresAt = Instant.now().plusSeconds(properties.getTicketTtlSeconds());
		sessionStore.saveTicket(
			ticket,
			new SsoTicketPayload(
				principal.getUserId(),
				principal.getEmail(),
				principal.getName(),
				principal.getAvatarUrl(),
				principal.getRoles(),
				statePayload.getPageType(),
				expiresAt
			),
			expiresAt
		);
		authAuditService.log("SSO_TICKET_ISSUED", "SUCCESS", UUID.fromString(principal.getUserId()), AuthRequestContext.from(request), null);

		return URI.create(UriComponentsBuilder.fromUriString(statePayload.getRedirectUri())
			.queryParam("ticket", ticket)
			.build(true)
			.toUriString());
	}

	public URI resolveOAuthFailureRedirect(HttpServletRequest request) {
		try {
			SsoStatePayload statePayload = consumeOAuthState(request);
			return URI.create(UriComponentsBuilder.fromUriString(statePayload.getRedirectUri())
				.queryParam("error", "oauth_failed")
				.build(true)
				.toUriString());
		} catch (GlobalException ex) {
			log.warn("OAuth failure redirect skipped: state not found");
			return null;
		}
	}

	public SsoPrincipal getCurrentUser(HttpServletRequest request, String page) {
		if (page != null && !page.isBlank() && SsoPageType.from(page) == SsoPageType.ADMIN) {
			adminIpGuardService.validate(request);
		}

		String sessionId = cookieService.extractSessionId(request)
			.orElseThrow(() -> new GlobalException(ErrorCode.NEED_LOGIN));

		SsoSessionPayload payload = sessionStore.findSession(sessionId)
			.orElseThrow(() -> new GlobalException(ErrorCode.NEED_LOGIN));

		return new SsoPrincipal(
			payload.getUserId(),
			payload.getEmail(),
			payload.getName(),
			payload.getAvatarUrl(),
			payload.getRoles() == null ? List.of() : payload.getRoles()
		);
	}

	public org.springframework.http.ResponseEntity<Void> logout(HttpServletRequest request) {
		cookieService.extractSessionId(request).ifPresent(sessionId -> {
			sessionStore.findSession(sessionId).ifPresent(payload ->
				authAuditService.log("SSO_LOGOUT", "SUCCESS", UUID.fromString(payload.getUserId()), AuthRequestContext.from(request), null)
			);
			sessionStore.revokeSession(sessionId);
		});
		return cookieService.clearSessionCookie();
	}

	private SsoStatePayload consumeOAuthState(HttpServletRequest request) {
		String state = cookieService.extractOAuthState(request)
			.orElseThrow(() -> {
				log.warn("OAuth callback rejected: state cookie missing");
				return new GlobalException(ErrorCode.INVALID_REQUEST);
			});

		return sessionStore.consumeState(state)
			.orElseThrow(() -> {
				log.warn("OAuth callback rejected: state not found or expired. state={}", state);
				return new GlobalException(ErrorCode.INVALID_REQUEST);
			});
	}

	private SsoTargetPage resolveTargetPage(String page, String redirectUri) {
		if (page != null && !page.isBlank()) {
			SsoPageType pageType = SsoPageType.from(page);
			String configuredRedirectUri = getConfiguredRedirectUri(pageType);
			if (redirectUri != null && !redirectUri.isBlank()) {
				validateRedirectUri(redirectUri, configuredRedirectUri);
			}
			return new SsoTargetPage(pageType, configuredRedirectUri);
		}

		if (redirectUri == null || redirectUri.isBlank()) {
			throw new GlobalException(ErrorCode.INVALID_REQUEST);
		}

		String normalized = normalizeRedirectUri(redirectUri);
		for (SsoPageType pageType : SsoPageType.values()) {
			String configuredRedirectUri = getConfiguredRedirectUri(pageType);
			if (normalizeRedirectUri(configuredRedirectUri).equals(normalized)) {
				return new SsoTargetPage(pageType, configuredRedirectUri);
			}
		}

		throw new GlobalException(ErrorCode.INVALID_REQUEST);
	}

	private void validateRedirectUri(String redirectUri, String expectedRedirectUri) {
		if (!normalizeRedirectUri(redirectUri).equals(normalizeRedirectUri(expectedRedirectUri))) {
			throw new GlobalException(ErrorCode.INVALID_REQUEST);
		}
	}

	private String getConfiguredRedirectUri(SsoPageType pageType) {
		return switch (pageType) {
			case EXPLAIN -> properties.getFrontend().getExplain().getRedirectUri();
			case EDITOR -> properties.getFrontend().getEditor().getRedirectUri();
			case ADMIN -> properties.getFrontend().getAdmin().getRedirectUri();
		};
	}

	private String normalizeRedirectUri(String redirectUri) {
		URI requested = URI.create(redirectUri);
		String path = requested.getPath();
		if (path == null || path.isBlank()) {
			path = "/";
		}

		return UriComponentsBuilder.newInstance()
			.scheme(requested.getScheme())
			.host(requested.getHost())
			.port(requested.getPort())
			.path(path)
			.build()
			.toUriString();
	}

	private String resolvePrimaryRole(SsoSessionPayload payload) {
		if (payload.getRoles() == null || payload.getRoles().isEmpty()) {
			return "";
		}
		String role = payload.getRoles().get(0);
		return role == null ? "" : role;
	}
}
