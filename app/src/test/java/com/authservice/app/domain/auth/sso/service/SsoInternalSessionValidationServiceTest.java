package com.authservice.app.domain.auth.sso.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.authservice.app.domain.auth.model.AuthAccountStatus;
import com.authservice.app.domain.auth.sso.dto.SsoResponse;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoSessionPayload;
import com.authservice.app.domain.auth.support.AuthAccessTokenResolver;
import com.authservice.app.domain.auth.userdirectory.model.UserAccountProfile;
import com.authservice.app.domain.auth.userdirectory.service.UserDirectory;
import io.github.jho951.platform.security.auth.PlatformAuthenticatedPrincipal;
import io.github.jho951.platform.security.auth.PlatformSessionSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SsoInternalSessionValidationServiceTest {

	@Test
	void returnsSessionValidationWhenSessionExists() {
		SsoSessionStore sessionStore = mock(SsoSessionStore.class);
		SsoCookieService cookieService = mock(SsoCookieService.class);
		AuthAccessTokenResolver accessTokenResolver = mock(AuthAccessTokenResolver.class);
		PlatformSessionSupport platformSessionSupport = mock(PlatformSessionSupport.class);
		UserDirectory userDirectory = mock(UserDirectory.class);
		SsoInternalSessionValidationService service = new SsoInternalSessionValidationService(
			sessionStore,
			cookieService,
			accessTokenResolver,
			platformSessionSupport,
			userDirectory
		);
		HttpServletRequest request = mock(HttpServletRequest.class);
		SsoSessionPayload payload = new SsoSessionPayload(
			"user-1",
			"user@example.com",
			"User",
			null,
			List.of("USER"),
			AuthAccountStatus.ACTIVE.code(),
			Instant.now().plusSeconds(60)
		);
		when(cookieService.extractSessionId(request)).thenReturn(Optional.of("session-1"));
		when(sessionStore.findSession("session-1")).thenReturn(Optional.of(payload));

		var response = service.validate(request);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().isAuthenticated()).isTrue();
		assertThat(response.getBody().getUserId()).isEqualTo("user-1");
		assertThat(response.getBody().getRole()).isEqualTo("USER");
		assertThat(response.getBody().getStatus()).isEqualTo(AuthAccountStatus.ACTIVE.code());
		assertThat(response.getBody().getSessionId()).isEqualTo("session-1");
	}

	@Test
	void fallsBackToJwtAuthenticationWhenSessionIsMissing() {
		SsoSessionStore sessionStore = mock(SsoSessionStore.class);
		SsoCookieService cookieService = mock(SsoCookieService.class);
		AuthAccessTokenResolver accessTokenResolver = mock(AuthAccessTokenResolver.class);
		PlatformSessionSupport platformSessionSupport = mock(PlatformSessionSupport.class);
		UserDirectory userDirectory = mock(UserDirectory.class);
		SsoInternalSessionValidationService service = new SsoInternalSessionValidationService(
			sessionStore,
			cookieService,
			accessTokenResolver,
			platformSessionSupport,
			userDirectory
		);
		HttpServletRequest request = mock(HttpServletRequest.class);
		UUID userId = UUID.randomUUID();
		when(cookieService.extractSessionId(request)).thenReturn(Optional.empty());
		when(accessTokenResolver.resolve(request)).thenReturn(Optional.of("access-token"));
		when(platformSessionSupport.authenticateAccessToken("access-token")).thenReturn(Optional.of(new PlatformAuthenticatedPrincipal(
			userId.toString(),
			java.util.Set.of("ROLE_USER"),
			java.util.Map.of()
		)));
		when(userDirectory.findByUserId(userId)).thenReturn(Optional.of(
			new UserAccountProfile(userId, "user@example.com", "User", "ROLE_USER", "ACTIVE", null)
		));

		var response = service.validate(request);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().isAuthenticated()).isTrue();
		assertThat(response.getBody().getUserId()).isEqualTo(userId.toString());
		assertThat(response.getBody().getRole()).isEqualTo("ROLE_USER");
		assertThat(response.getBody().getStatus()).isEqualTo("ACTIVE");
		assertThat(response.getBody().getSessionId()).isEmpty();
	}
}
