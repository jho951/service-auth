package com.authservice.app.domain.auth.sso.service;

import com.authservice.app.domain.auth.config.AuthHttpProperties;
import com.authservice.app.domain.auth.sso.config.SsoProperties;
import com.authservice.app.domain.auth.sso.model.GithubUserProfile;
import com.authservice.app.domain.auth.sso.model.SsoPageType;
import com.authservice.app.domain.auth.sso.model.SsoPrincipal;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoSessionPayload;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoStatePayload;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoTicketPayload;
import com.authservice.app.domain.auth.sso.model.SsoTargetPage;
import com.authservice.app.domain.auth.support.RefreshCookieWriter;
import com.authservice.app.domain.auth.support.RefreshTokenExtractor;
import com.authservice.app.domain.auth.model.AuthPrincipal;
import com.authservice.app.domain.auth.model.AuthTokens;
import com.authservice.app.domain.auth.service.AuthLoginService;
import com.authservice.app.domain.auth.service.AuthRedisRefreshTokenStore;
import com.authservice.app.domain.audit.service.AuthAuditLogService;
import com.authservice.app.security.AuthJwtTokenService;
import com.authservice.app.common.base.constant.ErrorCode;
import com.authservice.app.common.base.exception.GlobalException;
import com.authservice.app.domain.auth.userdirectory.service.UserDirectory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.util.ArrayList;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class SsoAuthService {

	private static final Logger log = LoggerFactory.getLogger(SsoAuthService.class);
	private static final String OAUTH_STATE_SESSION_KEY = "sso_oauth_state";

	private final SsoProperties properties;
	private final AuthHttpProperties authProperties;
	private final SsoSessionStore sessionStore;
	private final SsoUserService ssoUserService;
	private final UserDirectory userDirectory;
	private final AuthJwtTokenService tokenService;
	private final AuthLoginService authLoginService;
	private final AuthRedisRefreshTokenStore refreshTokenStore;
	private final RefreshCookieWriter refreshCookieWriter;
	private final RefreshTokenExtractor refreshTokenExtractor;
	private final SsoCookieService cookieService;
	private final AdminIpGuardService adminIpGuardService;
	private final AuthAuditLogService authAuditLogService;

	public SsoAuthService(
		SsoProperties properties,
		AuthHttpProperties authProperties,
		SsoSessionStore sessionStore,
		SsoUserService ssoUserService,
		UserDirectory userDirectory,
		AuthJwtTokenService tokenService,
		AuthLoginService authLoginService,
		AuthRedisRefreshTokenStore refreshTokenStore,
		RefreshCookieWriter refreshCookieWriter,
		RefreshTokenExtractor refreshTokenExtractor,
		SsoCookieService cookieService,
		AdminIpGuardService adminIpGuardService,
		AuthAuditLogService authAuditLogService
	) {
		this.properties = properties;
		this.authProperties = authProperties;
		this.sessionStore = sessionStore;
		this.ssoUserService = ssoUserService;
		this.userDirectory = userDirectory;
		this.tokenService = tokenService;
		this.authLoginService = authLoginService;
		this.refreshTokenStore = refreshTokenStore;
		this.refreshCookieWriter = refreshCookieWriter;
		this.refreshTokenExtractor = refreshTokenExtractor;
		this.cookieService = cookieService;
		this.adminIpGuardService = adminIpGuardService;
		this.authAuditLogService = authAuditLogService;
	}

	public org.springframework.http.ResponseEntity<Void> startGithubLogin(String page, String redirectUri, HttpServletRequest request) {
		SsoTargetPage targetPage = resolveTargetPage(page, redirectUri);
		if (targetPage.pageType() == SsoPageType.ADMIN) {
			adminIpGuardService.validate(request);
		}

		String state = UUID.randomUUID().toString();
		Instant expiresAt = Instant.now().plusSeconds(properties.getStateTtlSeconds());
		SsoStatePayload statePayload = new SsoStatePayload(targetPage.redirectUri(), targetPage.pageType().name(), expiresAt);
		sessionStore.saveState(
			state,
			statePayload,
			expiresAt
		);
		request.getSession(true).setAttribute(OAUTH_STATE_SESSION_KEY, statePayload);

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
				payload.getStatus(),
				expiresAt
			),
			expiresAt
		);
		authAuditLogService.logSsoLoginSuccess(payload.getUserId(), "github");

		AuthPrincipal principal = new AuthPrincipal(
			payload.getUserId(),
			payload.getRoles() == null ? List.of() : payload.getRoles(),
			java.util.Map.of(
				"email", payload.getEmail() == null ? "" : payload.getEmail(),
				"name", payload.getName() == null ? "" : payload.getName(),
				"avatarUrl", payload.getAvatarUrl() == null ? "" : payload.getAvatarUrl(),
				"status", payload.getStatus() == null ? "" : payload.getStatus()
			)
		);
		String accessToken = tokenService.issueAccessToken(principal);
		String refreshToken = tokenService.issueRefreshToken(principal);
		AuthTokens tokens = new AuthTokens(accessToken, refreshToken);
		refreshTokenStore.save(
			principal.userId(),
			refreshToken,
			Instant.now().plusSeconds(authProperties.getJwt().getRefreshSeconds())
		);
		String sessionCookie = cookieService.buildSessionCookie(sessionId);
		String accessTokenCookie = cookieService.buildAccessTokenCookie(accessToken);

		org.springframework.http.ResponseEntity<Void> refreshCookieResponse = refreshCookieWriter.write(
			tokens,
			org.springframework.http.ResponseEntity.noContent().build()
		);

		HttpHeaders mergedHeaders = new HttpHeaders();
		refreshCookieResponse.getHeaders().forEach((key, values) -> {
			if (values == null) {
				return;
			}
			for (String value : values) {
				mergedHeaders.add(key, value);
			}
		});
		mergedHeaders.add(HttpHeaders.SET_COOKIE, sessionCookie);
		mergedHeaders.add(HttpHeaders.SET_COOKIE, accessTokenCookie);

		return org.springframework.http.ResponseEntity.status(refreshCookieResponse.getStatusCode())
			.headers(mergedHeaders)
			.build();
	}

	public org.springframework.http.ResponseEntity<com.authservice.app.domain.auth.sso.dto.SsoResponse.InternalSessionValidationResponse> validateInternalSession(
		HttpServletRequest request
	) {
		String sessionId = cookieService.extractSessionId(request).orElse(null);
		if (sessionId != null && !sessionId.isBlank()) {
			Optional<SsoSessionPayload> sessionPayload = sessionStore.findSession(sessionId);
			if (sessionPayload.isPresent()) {
				SsoSessionPayload payload = sessionPayload.get();
				return org.springframework.http.ResponseEntity.ok(
					new com.authservice.app.domain.auth.sso.dto.SsoResponse.InternalSessionValidationResponse(
						true,
						payload.getUserId(),
						resolvePrimaryRole(payload),
						payload.getStatus(),
						sessionId
					)
				);
			}
		}

		Optional<com.authservice.app.domain.auth.sso.dto.SsoResponse.InternalSessionValidationResponse> jwtValidation =
			resolveFromJwtAuthentication();
		if (jwtValidation.isPresent()) {
			return org.springframework.http.ResponseEntity.ok(jwtValidation.get());
		}

		return org.springframework.http.ResponseEntity.status(401)
			.body(new com.authservice.app.domain.auth.sso.dto.SsoResponse.InternalSessionValidationResponse(false, "", "", "", ""));
	}

	public URI completeOAuthLogin(SsoPrincipal principal, HttpServletRequest request) {
		SsoStatePayload statePayload = consumeOAuthState(request);
		if (SsoPageType.from(statePayload.getPageType()) == SsoPageType.ADMIN) {
			adminIpGuardService.validate(request);
		}

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
				principal.getStatus(),
				statePayload.getPageType(),
				expiresAt
			),
			expiresAt
		);

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
			payload.getRoles() == null ? List.of() : payload.getRoles(),
			payload.getStatus()
		);
	}

	public org.springframework.http.ResponseEntity<Void> logout(HttpServletRequest request) {
		String actorId = cookieService.extractSessionId(request)
			.flatMap(sessionStore::findSession)
			.map(SsoSessionPayload::getUserId)
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
		authAuditLogService.logLogout(actorId, "SSO");
		org.springframework.http.ResponseEntity<Void> response = org.springframework.http.ResponseEntity.noContent()
			.header(HttpHeaders.SET_COOKIE, cookieService.clearSessionCookieValue())
			.header(HttpHeaders.SET_COOKIE, cookieService.clearAccessTokenCookie())
			.build();
		return refreshCookieWriter.clear(response);
	}

	private SsoStatePayload consumeOAuthState(HttpServletRequest request) {
		Optional<SsoStatePayload> sessionPayload = extractOAuthStatePayloadFromSession(request);
		if (sessionPayload.isPresent()) {
			return sessionPayload.get();
		}

		List<String> candidates = new ArrayList<>();
		cookieService.extractOAuthState(request).ifPresent(candidates::add);
		extractOAuthStateFromRequest(request).ifPresent(candidates::add);

		if (candidates.isEmpty()) {
			log.warn("OAuth callback rejected: state cookie/session/query missing");
			throw new GlobalException(ErrorCode.INVALID_REQUEST);
		}

		for (String candidate : candidates) {
			Optional<SsoStatePayload> payload = sessionStore.consumeState(candidate);
			if (payload.isPresent()) {
				return payload.get();
			}
		}

		log.warn("OAuth callback rejected: state not found or expired. candidates={}", candidates);
		throw new GlobalException(ErrorCode.INVALID_REQUEST);
	}

	private Optional<String> extractOAuthStateFromRequest(HttpServletRequest request) {
		String state = request.getParameter("state");
		if (state == null || state.isBlank()) {
			return Optional.empty();
		}
		return Optional.of(state);
	}

	private Optional<SsoStatePayload> extractOAuthStatePayloadFromSession(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return Optional.empty();
		}
		Object value = session.getAttribute(OAUTH_STATE_SESSION_KEY);
		if (value instanceof SsoStatePayload sessionStatePayload) {
			session.removeAttribute(OAUTH_STATE_SESSION_KEY);
			return Optional.of(sessionStatePayload);
		}
		return Optional.empty();
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

	private Optional<com.authservice.app.domain.auth.sso.dto.SsoResponse.InternalSessionValidationResponse> resolveFromJwtAuthentication() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return Optional.empty();
		}

		String userId = authentication.getName();
		if (userId == null || userId.isBlank() || "anonymousUser".equalsIgnoreCase(userId)) {
			return Optional.empty();
		}

		String role = resolvePrimaryRole(authentication.getAuthorities());
		String status = resolveStatus(userId);
		return Optional.of(new com.authservice.app.domain.auth.sso.dto.SsoResponse.InternalSessionValidationResponse(
			true,
			userId,
			role,
			status,
			""
		));
	}

	private String resolveStatus(String userId) {
		try {
			return userDirectory.findByUserId(UUID.fromString(userId))
				.map(profile -> profile.status() == null || profile.status().isBlank() ? "A" : profile.status())
				.orElse("A");
		} catch (IllegalArgumentException ex) {
			return "A";
		} catch (RuntimeException ex) {
			return "A";
		}
	}

	private String resolvePrimaryRole(Collection<? extends GrantedAuthority> authorities) {
		if (authorities == null || authorities.isEmpty()) {
			return "";
		}
		for (GrantedAuthority authority : authorities) {
			if (authority == null || authority.getAuthority() == null) {
				continue;
			}
			String value = authority.getAuthority();
			if (!value.isBlank()) {
				return value;
			}
		}
		return "";
	}
}
