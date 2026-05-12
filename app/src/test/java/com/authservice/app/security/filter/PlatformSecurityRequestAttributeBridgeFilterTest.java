package com.authservice.app.security.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.authservice.app.domain.auth.config.AuthCookieProperties;
import com.authservice.app.domain.auth.config.AuthHttpProperties;
import com.authservice.app.domain.auth.sso.config.SsoProperties;
import com.authservice.app.security.route.AuthRoutePolicy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PlatformSecurityRequestAttributeBridgeFilterTest {

	@Test
	void bridgesSessionAndAccessTokenCookies() throws ServletException, IOException {
		PlatformSecurityRequestAttributeBridgeFilter filter = newFilter();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/session");
		request.setCookies(
			new jakarta.servlet.http.Cookie("sso_session", "session-123"),
			new jakarta.servlet.http.Cookie("ACCESS_TOKEN", "access-token-123")
		);

		run(filter, request);

		assertThat(request.getAttribute("auth.sessionId")).isEqualTo("session-123");
		assertThat(request.getAttribute("auth.accessToken")).isEqualTo("access-token-123");
	}

	@Test
	void bridgesInternalSecretAsPlatformInternalToken() throws ServletException, IOException {
		PlatformSecurityRequestAttributeBridgeFilter filter = newFilter();
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/auth/session/validate");
		request.addHeader(
			com.authservice.app.domain.auth.userdirectory.config.InternalApiProperties.INTERNAL_SECRET_HEADER,
			"local-internal-api-key"
		);

		run(filter, request);

		assertThat(request.getAttribute("auth.internalToken")).isEqualTo("local-internal-api-key");
	}

	@Test
	void bridgesBearerInternalProofAsPlatformInternalToken() throws ServletException, IOException {
		PlatformSecurityRequestAttributeBridgeFilter filter = newFilter();
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/auth/session/validate");
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer local-internal-api-key");

		run(filter, request);

		assertThat(request.getAttribute("auth.internalToken")).isEqualTo("local-internal-api-key");
	}

	private static PlatformSecurityRequestAttributeBridgeFilter newFilter() {
		SsoProperties ssoProperties = new SsoProperties();
		AuthCookieProperties authCookieProperties = new AuthCookieProperties(
			new AuthHttpProperties(),
			"ACCESS_TOKEN",
			true,
			false,
			"Lax",
			"/"
		);
		return new PlatformSecurityRequestAttributeBridgeFilter(
			ssoProperties,
			authCookieProperties,
			new AuthHttpProperties(),
			new AuthRoutePolicy()
		);
	}

	private static void run(
		PlatformSecurityRequestAttributeBridgeFilter filter,
		MockHttpServletRequest request
	) throws ServletException, IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = new MockFilterChain();
		filter.doFilter(request, response, chain);
	}
}
