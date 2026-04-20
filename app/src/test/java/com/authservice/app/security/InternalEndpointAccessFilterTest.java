package com.authservice.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.authservice.app.domain.auth.userdirectory.config.InternalApiProperties;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class InternalEndpointAccessFilterTest {

	private InternalEndpointAccessFilter filter;

	@BeforeEach
	void setUp() {
		InternalApiProperties properties = new InternalApiProperties();
		properties.setKey("test-internal-key");
		filter = new InternalEndpointAccessFilter(properties);
	}

	@Test
	void rejectsInternalRequestWithoutCallerProof() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/auth/accounts");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentAsString()).contains("\"code\":9101");
	}

	@Test
	void allowsInternalRequestWithInternalSecret() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/internal/session/validate");
		request.addHeader(InternalApiProperties.INTERNAL_SECRET_HEADER, "test-internal-key");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(chain.getRequest()).isSameAs(request);
	}

	@Test
	void allowsInternalRequestWithBearerSecret() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/auth/accounts");
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-internal-key");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(chain.getRequest()).isSameAs(request);
	}

	@Test
	void skipsPublicRequest() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(chain.getRequest()).isSameAs(request);
	}
}
