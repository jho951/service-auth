package com.authservice.app.domain.auth.sso.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.authservice.app.domain.auth.model.AuthAccountStatus;
import com.authservice.app.domain.auth.sso.model.SsoPrincipal;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoSessionPayload;
import com.authservice.common.base.exception.GlobalException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SsoCurrentUserQueryServiceTest {

	@Test
	void returnsCurrentUserFromSessionAndChecksAdminPageGuard() {
		SsoSessionStore sessionStore = mock(SsoSessionStore.class);
		SsoCookieService cookieService = mock(SsoCookieService.class);
		AdminIpGuardService adminIpGuardService = mock(AdminIpGuardService.class);
		SsoCurrentUserQueryService service = new SsoCurrentUserQueryService(sessionStore, cookieService, adminIpGuardService);
		HttpServletRequest request = mock(HttpServletRequest.class);
		SsoSessionPayload payload = new SsoSessionPayload(
			"user-1",
			"user@example.com",
			"User",
			"https://example.com/avatar.png",
			List.of("ADMIN"),
			AuthAccountStatus.ACTIVE.code(),
			Instant.now().plusSeconds(60)
		);
		when(cookieService.extractSessionId(request)).thenReturn(Optional.of("session-1"));
		when(sessionStore.findSession("session-1")).thenReturn(Optional.of(payload));

		SsoPrincipal principal = service.getCurrentUser(request, "admin");

		assertThat(principal.getUserId()).isEqualTo("user-1");
		assertThat(principal.getRoles()).containsExactly("ADMIN");
		verify(adminIpGuardService).validate(request);
	}

	@Test
	void throwsNeedLoginWhenSessionIsMissing() {
		SsoCookieService cookieService = mock(SsoCookieService.class);
		when(cookieService.extractSessionId(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());
		SsoCurrentUserQueryService service = new SsoCurrentUserQueryService(
			mock(SsoSessionStore.class),
			cookieService,
			mock(AdminIpGuardService.class)
		);

		assertThatThrownBy(() -> service.getCurrentUser(mock(HttpServletRequest.class), null))
			.isInstanceOf(GlobalException.class);
	}
}
