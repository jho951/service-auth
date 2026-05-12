package com.authservice.app.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.authservice.app.domain.auth.sso.config.SsoProperties;
import com.authservice.app.domain.auth.sso.service.SsoOAuth2FailureHandler;
import com.authservice.app.domain.auth.sso.service.SsoOAuth2SuccessHandler;
import com.authservice.app.security.filter.CookieCsrfOriginGuardFilter;
import com.authservice.app.security.filter.PlatformSecurityRequestAttributeBridgeFilter;
import com.authservice.app.security.route.AuthRoutePolicy;
import com.authservice.common.security.RestAuthHandlers;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

class SecurityRouteClassificationTest {

	private SecurityConfig securityConfig;
	private AuthRoutePolicy authRoutePolicy;
	private Environment environment;

	@BeforeEach
	void setUp() {
		authRoutePolicy = new AuthRoutePolicy();
		environment = mock(Environment.class);
		when(environment.getProperty("springdoc.api-docs.enabled", Boolean.class, false)).thenReturn(false);
		securityConfig = new SecurityConfig(
			new RestAuthHandlers.EntryPoint(new ObjectMapper()),
			new RestAuthHandlers.Denied(new ObjectMapper()),
			new SsoProperties(),
			mock(SsoOAuth2SuccessHandler.class),
			mock(SsoOAuth2FailureHandler.class),
			mock(Filter.class),
			mock(PlatformSecurityRequestAttributeBridgeFilter.class),
			mock(CookieCsrfOriginGuardFilter.class),
			environment,
			authRoutePolicy
		);
	}

	@Test
	void classifiesPublicRoutes() {
		String[] publicRoutes = ReflectionTestUtils.invokeMethod(securityConfig, "publicRequestMatchers");

		assertThat(publicRoutes)
			.contains(
				"/health",
				"/auth/login",
				"/auth/refresh",
				"/auth/logout",
				"/auth/me",
				"/auth/session"
			)
			.doesNotContain("/internal/auth/**");
	}

	@Test
	void includesDocsRoutesWhenDocsAreEnabled() {
		when(environment.getProperty("springdoc.api-docs.enabled", Boolean.class, false)).thenReturn(true);

		String[] publicRoutes = ReflectionTestUtils.invokeMethod(securityConfig, "publicRequestMatchers");

		assertThat(publicRoutes)
			.contains(
				"/v3/api-docs/**",
				"/swagger-ui/**",
				"/swagger-ui.html"
			);
	}

	@Test
	void classifiesProtectedRoutes() {
		String[] protectedRoutes = ReflectionTestUtils.invokeMethod(securityConfig, "protectedRequestMatchers");

		assertThat(protectedRoutes).containsExactly("/api/**");
	}

	@Test
	void classifiesAdminRoutes() {
		String[] adminRoutes = ReflectionTestUtils.invokeMethod(securityConfig, "adminRequestMatchers");

		assertThat(adminRoutes).containsExactly("/admin/**");
	}

	@Test
	void classifiesInternalRoutes() {
		String[] internalRoutes = ReflectionTestUtils.invokeMethod(securityConfig, "internalPassThroughRequestMatchers");

		assertThat(internalRoutes).containsExactly("/internal/**");
	}
}
