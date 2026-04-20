package com.authservice.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.authservice.app.domain.auth.config.AuthHttpProperties;
import com.authservice.app.domain.auth.sso.config.SsoProperties;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CookieCsrfOriginGuardFilterTest {

	private CookieCsrfOriginGuardFilter filter;

	@BeforeEach
	void setUp() {
		filter = new CookieCsrfOriginGuardFilter(new SsoProperties(), new AuthHttpProperties(), "ACCESS_TOKEN");
	}

	@Test
	void allowsCookieRefreshFromAllowedOrigin() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/refresh");
		request.setCookies(new jakarta.servlet.http.Cookie("refresh_token", "refresh-token"));
		request.addHeader(HttpHeaders.ORIGIN, "http://localhost:3000");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(chain.getRequest()).isSameAs(request);
	}

	@Test
	void rejectsCookieRefreshFromUntrustedOrigin() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/refresh");
		request.setCookies(new jakarta.servlet.http.Cookie("refresh_token", "refresh-token"));
		request.addHeader(HttpHeaders.ORIGIN, "https://evil.example");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentAsString()).contains("\"code\":9102");
	}

	@Test
	void rejectsCookieLogoutWithoutOriginProof() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/logout");
		request.setCookies(new jakarta.servlet.http.Cookie("sso_session", "session-id"));
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(403);
	}

	@Test
	void allowsBearerOnlyRefreshWithoutCookieOriginCheck() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/refresh");
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(chain.getRequest()).isSameAs(request);
	}
}
