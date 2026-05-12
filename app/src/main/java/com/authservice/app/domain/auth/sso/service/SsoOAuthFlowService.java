package com.authservice.app.domain.auth.sso.service;

import com.authservice.app.domain.auth.config.AuthHttpProperties;
import com.authservice.app.domain.auth.model.OAuthProvider;
import com.authservice.app.domain.auth.sso.config.SsoProperties;
import com.authservice.app.domain.auth.sso.model.SsoPrincipal;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoTicketPayload;
import com.authservice.common.base.exception.GlobalException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class SsoOAuthFlowService {

	private static final Logger log = LoggerFactory.getLogger(SsoOAuthFlowService.class);

	private final SsoProperties properties;
	private final AuthHttpProperties authProperties;
	private final SsoSessionStore sessionStore;
	private final SsoTargetPageResolver targetPageResolver;
	private final SsoOAuthStateService oAuthStateService;

	public SsoOAuthFlowService(
		SsoProperties properties,
		AuthHttpProperties authProperties,
		SsoSessionStore sessionStore,
		SsoTargetPageResolver targetPageResolver,
		SsoOAuthStateService oAuthStateService
	) {
		this.properties = properties;
		this.authProperties = authProperties;
		this.sessionStore = sessionStore;
		this.targetPageResolver = targetPageResolver;
		this.oAuthStateService = oAuthStateService;
	}

	public ResponseEntity<Void> startGithubLogin(String page, String redirectUri, HttpServletRequest request) {
		var targetPage = targetPageResolver.resolve(page, redirectUri);

		String authorizationUri = UriComponentsBuilder
			.fromPath(authProperties.getOauth2().getAuthorizationBaseUri())
			.pathSegment(OAuthProvider.GITHUB.externalName())
			.build()
			.toUriString();

		return oAuthStateService.prepareStart(targetPage, request)
			.location(URI.create(authorizationUri))
			.build();
	}

	public URI completeOAuthLogin(SsoPrincipal principal, HttpServletRequest request) {
		var statePayload = oAuthStateService.consume(request);
		oAuthStateService.enforceAdminIpGuard(statePayload.getPageType(), request);

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

		return targetPageResolver.successRedirect(statePayload.getRedirectUri(), ticket);
	}

	public URI resolveOAuthFailureRedirect(HttpServletRequest request) {
		try {
			var statePayload = oAuthStateService.consume(request);
			return targetPageResolver.failureRedirect(statePayload.getRedirectUri());
		} catch (GlobalException ex) {
			log.warn("OAuth failure redirect skipped: state not found");
			return null;
		}
	}
}
