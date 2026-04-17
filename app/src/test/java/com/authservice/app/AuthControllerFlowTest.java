package com.authservice.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.authservice.app.domain.auth.controller.AuthController;
import com.authservice.app.domain.auth.dto.AuthRequest;
import com.authservice.app.domain.auth.dto.AuthResponse;
import com.authservice.app.domain.auth.model.AuthTokens;
import com.authservice.app.domain.auth.sso.service.SsoCookieService;
import com.authservice.app.domain.auth.service.AuthAccountPolicyService;
import com.authservice.app.domain.auth.service.AuthLoginService;
import com.authservice.app.domain.auth.service.AuthLoginAttemptService;
import com.authservice.app.domain.auth.support.RefreshCookieWriter;
import com.authservice.app.domain.auth.support.RefreshTokenExtractor;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AuthControllerFlowTest {

	@Mock
	private AuthLoginService authService;

	@Mock
	private RefreshTokenExtractor refreshTokenExtractor;

	@Mock
	private RefreshCookieWriter refreshCookieWriter;

	@Mock
	private AuthAccountPolicyService authAccountPolicyService;

	@Mock
	private AuthLoginAttemptService authLoginAttemptService;

	@Mock
	private SsoCookieService ssoCookieService;

	private AuthController authController;

	@BeforeEach
	void setUp() {
		authController = new AuthController(
			authService,
			refreshTokenExtractor,
			refreshCookieWriter,
			authAccountPolicyService,
			authLoginAttemptService,
			ssoCookieService
		);
	}

	@Test
	void loginSuccessReturnsAccessAndRefreshTokenPair() {
		AuthRequest.LoginRequest request = new AuthRequest.LoginRequest("user@example.com", "Password12!");
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		AuthTokens tokens = new AuthTokens("access-token", "refresh-token");
		when(ssoCookieService.buildAccessTokenCookie("access-token")).thenReturn("access=access-token");
		when(authService.login(request.getUsername(), request.getPassword())).thenReturn(tokens);
		when(refreshCookieWriter.write(
			any(AuthTokens.class),
			anyTokenResponseEntity()
		))
			.thenReturn(ResponseEntity.ok().build());

		ResponseEntity<AuthResponse.TokenResponse> response = authController.login(request, servletRequest);

		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getAccessToken()).isEqualTo("access-token");
		assertThat(response.getBody().getRefreshToken()).isEqualTo("refresh-token");
		verify(authAccountPolicyService).markLoginSuccess(request.getUsername());
		verify(authLoginAttemptService).record(eq(request.getUsername()), any(), eq("SUCCESS"));
	}

	@Test
	void loginFailureRecordsFailureAuditAndRethrows() {
		AuthRequest.LoginRequest request = new AuthRequest.LoginRequest("user@example.com", "wrong-password");
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		RuntimeException loginException = new RuntimeException("invalid credentials");
		when(authService.login(request.getUsername(), request.getPassword())).thenThrow(loginException);

		assertThatThrownBy(() -> authController.login(request, servletRequest))
			.isSameAs(loginException);

		verify(authAccountPolicyService).markLoginFailure(request.getUsername());
		verify(authLoginAttemptService).record(eq(request.getUsername()), any(), eq("FAILURE"));
	}

	@Test
	void refreshReturnsRotatedTokenPair() {
		HttpServletRequest servletRequest = new MockHttpServletRequest();
		AuthTokens refreshed = new AuthTokens("new-access-token", "new-refresh-token");
		when(ssoCookieService.buildAccessTokenCookie("new-access-token")).thenReturn("access=new-access-token");
		when(refreshTokenExtractor.extract(servletRequest)).thenReturn("old-refresh-token");
		when(authService.refresh("old-refresh-token")).thenReturn(refreshed);
		when(refreshCookieWriter.write(
			any(AuthTokens.class),
			anyTokenResponseEntity()
		))
			.thenReturn(ResponseEntity.ok().build());

		ResponseEntity<AuthResponse.TokenResponse> response = authController.refresh(servletRequest);

		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getAccessToken()).isEqualTo("new-access-token");
		assertThat(response.getBody().getRefreshToken()).isEqualTo("new-refresh-token");
		verify(authService).refresh("old-refresh-token");
	}

	@Test
	void logoutRevokesRefreshTokenAndClearsCookie() {
		HttpServletRequest servletRequest = new MockHttpServletRequest();
		when(refreshTokenExtractor.extract(servletRequest)).thenReturn("refresh-token");
		when(ssoCookieService.clearAccessTokenCookie()).thenReturn("access=; Max-Age=0");
		when(refreshCookieWriter.clear(anyVoidResponseEntity())).thenReturn(ResponseEntity.noContent().build());

		ResponseEntity<Void> response = authController.logout(servletRequest);

		assertThat(response.getStatusCode().value()).isEqualTo(204);
		verify(authService).logout("refresh-token");
		verify(refreshCookieWriter).clear(anyVoidResponseEntity());
	}

	private ResponseEntity<AuthResponse.TokenResponse> anyTokenResponseEntity() {
		return any();
	}

	private ResponseEntity<Void> anyVoidResponseEntity() {
		return any();
	}
}
