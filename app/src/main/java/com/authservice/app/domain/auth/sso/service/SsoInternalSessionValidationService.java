package com.authservice.app.domain.auth.sso.service;

import com.authservice.app.domain.auth.model.AuthAccountStatus;
import com.authservice.app.domain.auth.sso.dto.SsoResponse;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoSessionPayload;
import com.authservice.app.domain.auth.support.AuthAccessTokenResolver;
import com.authservice.app.domain.auth.userdirectory.service.UserDirectory;
import io.github.jho951.platform.security.auth.PlatformSessionSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class SsoInternalSessionValidationService {

	private static final SsoResponse.InternalSessionValidationResponse UNAUTHENTICATED_RESPONSE =
		new SsoResponse.InternalSessionValidationResponse(false, "", "", "", "");

	private final SsoSessionStore sessionStore;
	private final SsoCookieService cookieService;
	private final AuthAccessTokenResolver authAccessTokenResolver;
	private final PlatformSessionSupport platformSessionSupport;
	private final UserDirectory userDirectory;

	public SsoInternalSessionValidationService(
		SsoSessionStore sessionStore,
		SsoCookieService cookieService,
		AuthAccessTokenResolver authAccessTokenResolver,
		PlatformSessionSupport platformSessionSupport,
		UserDirectory userDirectory
	) {
		this.sessionStore = sessionStore;
		this.cookieService = cookieService;
		this.authAccessTokenResolver = authAccessTokenResolver;
		this.platformSessionSupport = platformSessionSupport;
		this.userDirectory = userDirectory;
	}

	public ResponseEntity<SsoResponse.InternalSessionValidationResponse> validate(HttpServletRequest request) {
		return resolveFromSession(request)
			.or(() -> resolveFromAccessToken(request))
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.status(401).body(UNAUTHENTICATED_RESPONSE));
	}

	private Optional<SsoResponse.InternalSessionValidationResponse> resolveFromSession(HttpServletRequest request) {
		String sessionId = cookieService.extractSessionId(request).orElse(null);
		if (sessionId == null || sessionId.isBlank()) {
			return Optional.empty();
		}

		return sessionStore.findSession(sessionId)
			.map(payload -> new SsoResponse.InternalSessionValidationResponse(
				true,
				payload.getUserId(),
				resolvePrimaryRole(payload.getRoles()),
				payload.getStatus(),
				sessionId
			));
	}

	private Optional<SsoResponse.InternalSessionValidationResponse> resolveFromAccessToken(HttpServletRequest request) {
		Optional<String> accessToken = authAccessTokenResolver.resolve(request);
		if (accessToken.isEmpty()) {
			return Optional.empty();
		}

		try {
			var principal = platformSessionSupport.authenticateAccessToken(accessToken.get()).orElse(null);
			if (principal == null) {
				return Optional.empty();
			}
			String userId = principal.userId();
			if (userId == null || userId.isBlank()) {
				return Optional.empty();
			}
			return Optional.of(new SsoResponse.InternalSessionValidationResponse(
				true,
				userId,
				resolvePrimaryRole(principal.authorities()),
				resolveStatus(userId),
				""
			));
		} catch (RuntimeException ex) {
			return Optional.empty();
		}
	}

	private String resolveStatus(String userId) {
		try {
			return userDirectory.findByUserId(UUID.fromString(userId))
				.map(profile -> AuthAccountStatus.normalizeOrDefault(profile.status()))
				.orElse(AuthAccountStatus.ACTIVE.code());
		} catch (IllegalArgumentException ex) {
			return AuthAccountStatus.ACTIVE.code();
		} catch (RuntimeException ex) {
			return AuthAccountStatus.ACTIVE.code();
		}
	}

	private String resolvePrimaryRole(Collection<String> roles) {
		if (roles == null || roles.isEmpty()) {
			return "";
		}
		String role = roles.iterator().next();
		return role == null ? "" : role;
	}
}
