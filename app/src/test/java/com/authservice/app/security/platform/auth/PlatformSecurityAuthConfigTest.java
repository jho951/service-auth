package com.authservice.app.security.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.authservice.app.domain.auth.model.AuthPrincipal;
import com.authservice.app.domain.auth.userdirectory.config.InternalApiProperties;
import com.authservice.app.domain.auth.support.AuthPrincipalNames;
import com.authservice.app.domain.auth.sso.service.SsoSessionStore;
import com.authservice.app.security.jwt.AuthJwtTokenService;
import com.authservice.app.security.platform.issuer.AuthPlatformIssuerAdaptersConfiguration;
import io.github.jho951.platform.security.auth.PlatformSessionSupportFactory;
import io.github.jho951.platform.security.api.SecurityContext;
import io.github.jho951.platform.security.api.SecurityContextResolver;
import io.github.jho951.platform.security.api.SecurityRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PlatformSecurityAuthConfigTest {

	@Test
	void resolvesInternalCallerAsInternalRole() {
		PlatformSecurityAuthConfig config = new PlatformSecurityAuthConfig();
		InternalApiProperties internalApiProperties = new InternalApiProperties();
		internalApiProperties.setKey("internal-key");
		SecurityContextResolver resolver = config.securityContextResolver(
			emptySupportFactory(),
			config.platformSecurityLocalInternalTokenClaimsValidator(),
			config.internalServiceCompatibilityAuthenticationAdapter(internalApiProperties)
		);

		SecurityContext context = resolver.resolve(new SecurityRequest(
			null,
			"127.0.0.1",
			"/internal/auth/session/validate",
			"POST",
			Map.of(
				"security.boundary", "INTERNAL",
				"auth.internalToken", "internal-key"
			),
			Instant.now()
		));

		assertThat(context.authenticated()).isTrue();
		assertThat(context.principal()).isEqualTo(AuthPrincipalNames.INTERNAL_SERVICE);
		assertThat(context.roles()).contains(AuthPrincipalNames.INTERNAL_ROLE);
		assertThat(context.attributes()).containsEntry("authType", "internal");
	}

	@Test
	void rejectsRegularJwtOnInternalBoundaryWithoutInternalProof() {
		PlatformSecurityAuthConfig config = new PlatformSecurityAuthConfig();
		AuthJwtTokenService tokenService = mockTokenService();
		PlatformSessionSupportFactory platformSessionSupportFactory =
			new AuthPlatformIssuerAdaptersConfiguration().platformSessionSupportFactory(
				tokenService,
				Mockito.mock(SsoSessionStore.class)
			);
		SecurityContextResolver resolver = config.securityContextResolver(
			platformSessionSupportFactory,
			config.platformSecurityLocalInternalTokenClaimsValidator(),
			config.internalServiceCompatibilityAuthenticationAdapter(new InternalApiProperties())
		);

		String accessToken = tokenService.issueAccessToken(new AuthPrincipal("user-1", List.of("ROLE_USER"), Map.of()));
		SecurityContext context = resolver.resolve(new SecurityRequest(
			null,
			"127.0.0.1",
			"/internal/auth/session/validate",
			"POST",
			Map.of(
				"security.boundary", "INTERNAL",
				"auth.accessToken", accessToken
			),
			Instant.now()
		));

		assertThat(context.authenticated()).isFalse();
		assertThat(context.principal()).isNull();
		assertThat(context.roles()).isEmpty();
	}

	private static AuthJwtTokenService mockTokenService() {
		return new AuthJwtTokenService(
			"12345678901234567890123456789012",
			"auth-service",
			1200L,
			30000L
		);
	}

	private static PlatformSessionSupportFactory emptySupportFactory() {
		return () -> (accessToken, sessionId) -> java.util.Optional.empty();
	}
}
