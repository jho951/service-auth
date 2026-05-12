package com.authservice.app.domain.auth.sso.service;

import com.authservice.app.domain.auth.sso.config.SsoProperties;
import com.authservice.app.domain.auth.sso.model.SsoPageType;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoStatePayload;
import com.authservice.app.domain.auth.sso.model.SsoTargetPage;
import com.authservice.common.base.constant.ErrorCode;
import com.authservice.common.base.exception.GlobalException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class SsoOAuthStateService {

	private static final Logger log = LoggerFactory.getLogger(SsoOAuthStateService.class);
	private static final String OAUTH_STATE_SESSION_KEY = "sso_oauth_state";

	private final SsoProperties properties;
	private final SsoSessionStore sessionStore;
	private final SsoCookieService cookieService;
	private final AdminIpGuardService adminIpGuardService;

	public SsoOAuthStateService(
		SsoProperties properties,
		SsoSessionStore sessionStore,
		SsoCookieService cookieService,
		AdminIpGuardService adminIpGuardService
	) {
		this.properties = properties;
		this.sessionStore = sessionStore;
		this.cookieService = cookieService;
		this.adminIpGuardService = adminIpGuardService;
	}

	public ResponseEntity.BodyBuilder prepareStart(SsoTargetPage targetPage, HttpServletRequest request) {
		enforceAdminIpGuard(targetPage.pageType(), request);

		String state = UUID.randomUUID().toString();
		Instant expiresAt = Instant.now().plusSeconds(properties.getStateTtlSeconds());
		SsoStatePayload statePayload = new SsoStatePayload(targetPage.redirectUri(), targetPage.pageType().name(), expiresAt);
		sessionStore.saveState(state, statePayload, expiresAt);
		request.getSession(true).setAttribute(OAUTH_STATE_SESSION_KEY, statePayload);

		return ResponseEntity.status(302)
			.header(HttpHeaders.SET_COOKIE, cookieService.buildOAuthStateCookie(state, properties.getStateTtlSeconds()));
	}

	public SsoStatePayload consume(HttpServletRequest request) {
		Optional<SsoStatePayload> sessionPayload = extractFromSession(request);
		if (sessionPayload.isPresent()) {
			return sessionPayload.get();
		}

		List<String> candidates = new ArrayList<>();
		cookieService.extractOAuthState(request).ifPresent(candidates::add);
		extractFromRequest(request).ifPresent(candidates::add);

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

	public void enforceAdminIpGuard(String pageType, HttpServletRequest request) {
		enforceAdminIpGuard(SsoPageType.from(pageType), request);
	}

	public void enforceAdminIpGuard(SsoPageType pageType, HttpServletRequest request) {
		if (pageType == SsoPageType.ADMIN) {
			adminIpGuardService.validate(request);
		}
	}

	private Optional<String> extractFromRequest(HttpServletRequest request) {
		String state = request.getParameter("state");
		if (state == null || state.isBlank()) {
			return Optional.empty();
		}
		return Optional.of(state);
	}

	private Optional<SsoStatePayload> extractFromSession(HttpServletRequest request) {
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
}
