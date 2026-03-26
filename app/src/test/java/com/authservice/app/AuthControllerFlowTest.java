package com.authservice.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auth.api.model.Tokens;
import com.auth.config.controller.RefreshCookieWriter;
import com.auth.config.controller.RefreshTokenExtractor;
import com.auth.config.dto.LoginResponse;
import com.auth.core.service.AuthService;
import com.authservice.app.domain.auth.controller.AuthController;
import com.authservice.app.domain.auth.dto.AuthRequest;
import com.authservice.app.domain.auth.dto.AuthResponse;
import com.authservice.app.domain.auth.service.AuthAccountPolicyService;
import com.authservice.app.domain.auth.service.AuthLoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AuthControllerFlowTest {

	@Mock
	private AuthService authService;

	@Mock
	private RefreshTokenExtractor refreshTokenExtractor;

	@Mock
	private RefreshCookieWriter refreshCookieWriter;

	@Mock
	private AuthAccountPolicyService authAccountPolicyService;

	@Mock
	private AuthLoginAttemptService authLoginAttemptService;

	private AuthController authController;

	@BeforeEach
	void setUp() {
		authController = new AuthController(
			authService,
			refreshTokenExtractor,
			refreshCookieWriter,
			authAccountPolicyService,
			authLoginAttemptService
		);
	}

	@Test
	void loginSuccessReturnsAccessAndRefreshTokenPair() {
		AuthRequest.LoginRequest request = new AuthRequest.LoginRequest("user@example.com", "Password12!");
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		Tokens tokens = mock(Tokens.class);
		when(tokens.getAccessToken()).thenReturn("access-token");
		when(tokens.getRefreshToken()).thenReturn("refresh-token");
		when(authService.login(request.getUsername(), request.getPassword())).thenReturn(tokens);
		when(refreshCookieWriter.write(any(Tokens.class), any(ResponseEntity.class)))
			.thenReturn(ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, "refresh=refresh-token").body(new LoginResponse("access-token")));

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
		Tokens refreshed = mock(Tokens.class);
		when(refreshed.getAccessToken()).thenReturn("new-access-token");
		when(refreshed.getRefreshToken()).thenReturn("new-refresh-token");
		when(refreshTokenExtractor.extract(servletRequest)).thenReturn("old-refresh-token");
		when(authService.refresh("old-refresh-token")).thenReturn(refreshed);
		when(refreshCookieWriter.write(any(Tokens.class), any(ResponseEntity.class)))
			.thenReturn(ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, "refresh=new-refresh-token").body(new LoginResponse("new-access-token")));

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
		when(refreshCookieWriter.clear(any(ResponseEntity.class))).thenReturn(ResponseEntity.noContent().build());

		ResponseEntity<Void> response = authController.logout(servletRequest);

		assertThat(response.getStatusCode().value()).isEqualTo(204);
		verify(authService).logout("refresh-token");
		verify(refreshCookieWriter).clear(any(ResponseEntity.class));
	}
}
