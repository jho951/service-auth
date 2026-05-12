package com.authservice.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.authservice.app.domain.auth.sso.controller.SsoController;
import com.authservice.app.domain.auth.sso.dto.SsoRequest;
import com.authservice.app.domain.auth.sso.dto.SsoResponse;
import com.authservice.app.domain.auth.sso.model.SsoPrincipal;
import com.authservice.app.domain.auth.sso.service.SsoCurrentUserQueryService;
import com.authservice.app.domain.auth.sso.service.SsoInternalSessionValidationService;
import com.authservice.app.domain.auth.sso.service.SsoLogoutService;
import com.authservice.app.domain.auth.sso.service.SsoOAuthFlowService;
import com.authservice.app.domain.auth.sso.service.SsoTicketExchangeService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class SsoControllerFlowTest {

	@Mock
	private SsoOAuthFlowService ssoOAuthFlowService;

	@Mock
	private SsoTicketExchangeService ssoTicketExchangeService;

	@Mock
	private SsoInternalSessionValidationService ssoInternalSessionValidationService;

	@Mock
	private SsoCurrentUserQueryService ssoCurrentUserQueryService;

	@Mock
	private SsoLogoutService ssoLogoutService;

	private SsoController ssoController;

	@BeforeEach
	void setUp() {
		ssoController = new SsoController(
			ssoOAuthFlowService,
			ssoTicketExchangeService,
			ssoInternalSessionValidationService,
			ssoCurrentUserQueryService,
			ssoLogoutService
		);
	}

	@Test
	void startGithubLoginDelegatesToOAuthFlowService() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		when(ssoOAuthFlowService.startGithubLogin("editor", "http://localhost/auth/callback", request))
			.thenReturn(ResponseEntity.status(302).build());

		ResponseEntity<Void> response = ssoController.startGithubLogin("editor", "http://localhost/auth/callback", request);

		assertThat(response.getStatusCode().value()).isEqualTo(302);
		verify(ssoOAuthFlowService).startGithubLogin("editor", "http://localhost/auth/callback", request);
	}

	@Test
	void exchangeDelegatesToTicketExchangeService() {
		SsoRequest.ExchangeRequest request = new SsoRequest.ExchangeRequest();
		request.setTicket("ticket-1");
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		when(ssoTicketExchangeService.exchangeTicket("ticket-1", servletRequest)).thenReturn(ResponseEntity.noContent().build());

		ResponseEntity<Void> response = ssoController.exchange(request, servletRequest);

		assertThat(response.getStatusCode().value()).isEqualTo(204);
		verify(ssoTicketExchangeService).exchangeTicket("ticket-1", servletRequest);
	}

	@Test
	void logoutDelegatesToSsoLogoutService() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		when(ssoLogoutService.logout(request)).thenReturn(ResponseEntity.noContent().build());

		ResponseEntity<Void> response = ssoController.logout(request);

		assertThat(response.getStatusCode().value()).isEqualTo(204);
		verify(ssoLogoutService).logout(request);
	}

	@Test
	void sessionDelegatesToInternalSessionValidationService() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		SsoResponse.InternalSessionValidationResponse validation =
			new SsoResponse.InternalSessionValidationResponse(true, "user-1", "USER", "ACTIVE", "session-1");
		when(ssoInternalSessionValidationService.validate(request)).thenReturn(ResponseEntity.ok(validation));

		ResponseEntity<SsoResponse.InternalSessionValidationResponse> response = ssoController.session(request);

		assertThat(response.getBody()).isSameAs(validation);
		verify(ssoInternalSessionValidationService).validate(request);
	}

	@Test
	void meReturnsCurrentUserSummary() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		SsoPrincipal principal = new SsoPrincipal(
			"user-1",
			"user@example.com",
			"User",
			"https://example.com/avatar.png",
			List.of("USER"),
			"ACTIVE"
		);
		when(ssoCurrentUserQueryService.getCurrentUser(request, "editor")).thenReturn(principal);

		SsoResponse.MeResponse response = ssoController.me(request, "editor");

		assertThat(response.getId()).isEqualTo("user-1");
		assertThat(response.getEmail()).isEqualTo("user@example.com");
		assertThat(response.getRoles()).containsExactly("USER");
		verify(ssoCurrentUserQueryService).getCurrentUser(request, "editor");
	}
}
