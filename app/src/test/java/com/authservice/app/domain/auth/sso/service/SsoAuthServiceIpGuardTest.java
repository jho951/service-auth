package com.authservice.app.domain.auth.sso.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.authservice.app.domain.auth.config.AuthHttpProperties;
import com.authservice.app.domain.auth.sso.config.SsoProperties;
import com.authservice.app.domain.auth.sso.model.SsoPageType;
import com.authservice.app.domain.auth.sso.model.SsoPrincipal;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoStatePayload;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoTicketPayload;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SsoAuthServiceIpGuardTest {

	@Test
	void completeOAuthLoginChecksAdminIpGuardBeforeIssuingTicket() {
		SsoSessionStore sessionStore = mock(SsoSessionStore.class);
		AdminIpGuardService adminIpGuardService = mock(AdminIpGuardService.class);
		SsoAuthService service = service(sessionStore, adminIpGuardService);
		HttpServletRequest request = requestWithState(SsoPageType.ADMIN);
		RuntimeException blocked = new RuntimeException("blocked");
		doThrow(blocked).when(adminIpGuardService).validate(request);

		assertThatThrownBy(() -> service.completeOAuthLogin(principal(), request))
			.isSameAs(blocked);

		verify(adminIpGuardService).validate(request);
		verify(sessionStore, never()).saveTicket(any(), any(SsoTicketPayload.class), any());
	}

	private SsoAuthService service(SsoSessionStore sessionStore, AdminIpGuardService adminIpGuardService) {
		return new SsoAuthService(
			new SsoProperties(),
			new AuthHttpProperties(),
			sessionStore,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			mock(SsoCookieService.class),
			adminIpGuardService,
			null
		);
	}

	private HttpServletRequest requestWithState(SsoPageType pageType) {
		HttpSession session = mock(HttpSession.class);
		when(session.getAttribute("sso_oauth_state"))
			.thenReturn(new SsoStatePayload("http://localhost/callback", pageType.name(), Instant.now().plusSeconds(60)));

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getSession(false)).thenReturn(session);
		return request;
	}

	private SsoPrincipal principal() {
		return new SsoPrincipal("user-1", "user@example.com", "User", "", List.of("ADMIN"), "A");
	}
}
