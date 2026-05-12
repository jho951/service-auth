package com.authservice.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.authservice.app.domain.audit.service.AuthAuditLogService;
import com.authservice.app.domain.auth.model.AuthFailureReason;
import com.authservice.app.domain.auth.controller.AuthGatewayController;
import com.authservice.app.domain.auth.dto.AuthRequest;
import com.authservice.app.domain.auth.dto.AuthResponse;
import com.authservice.app.domain.auth.model.AuthLoginResult;
import com.authservice.app.domain.auth.model.AuthTokens;
import com.authservice.app.domain.auth.service.AuthAccountPolicyService;
import com.authservice.app.domain.auth.service.AuthLoginAttemptService;
import com.authservice.app.domain.auth.service.AuthLoginService;
import com.authservice.app.domain.auth.support.AuthCookieResponseWriter;
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
class AuthGatewayControllerFlowTest {

	@Mock
	private AuthLoginService authService;

	@Mock
	private RefreshTokenExtractor refreshTokenExtractor;

	@Mock
	private AuthCookieResponseWriter authCookieResponseWriter;

	@Mock
	private AuthAccountPolicyService authAccountPolicyService;

	@Mock
	private AuthLoginAttemptService authLoginAttemptService;

	@Mock
	private AuthAuditLogService authAuditLogService;

	private AuthGatewayController authGatewayController;

	@BeforeEach
	void setUp() {
		authGatewayController = new AuthGatewayController(
			authService,
			refreshTokenExtractor,
			authCookieResponseWriter,
			authAccountPolicyService,
			authLoginAttemptService,
			authAuditLogService
		);
	}

	@Test
	void loginSuccessReturnsAccessAndRefreshTokenPair() {
		AuthRequest.LoginRequest request = new AuthRequest.LoginRequest("user@example.com", "Password12!");
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		AuthTokens tokens = new AuthTokens("access-token", "refresh-token");
		when(authService.login(request.getUsername(), request.getPassword())).thenReturn(tokens);
		ResponseEntity<AuthResponse.TokenResponse> expected = ResponseEntity.ok(new AuthResponse.TokenResponse("access-token", "refresh-token"));
		when(authCookieResponseWriter.writeTokenCookies(any(AuthTokens.class), anyTokenResponseEntity())).thenReturn(expected);

		ResponseEntity<AuthResponse.TokenResponse> response = authGatewayController.login(request, servletRequest);

		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getAccessToken()).isEqualTo("access-token");
		assertThat(response.getBody().getRefreshToken()).isEqualTo("refresh-token");
		verify(authAccountPolicyService).markLoginSuccess(request.getUsername());
		verify(authLoginAttemptService).record(eq(request.getUsername()), any(), eq(AuthLoginResult.SUCCESS));
		verify(authAuditLogService).logPasswordLoginSuccess(request.getUsername());
	}

	@Test
	void loginFailureRecordsFailureAuditAndRethrows() {
		AuthRequest.LoginRequest request = new AuthRequest.LoginRequest("user@example.com", "wrong-password");
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		RuntimeException loginException = new RuntimeException("invalid credentials");
		when(authService.login(request.getUsername(), request.getPassword())).thenThrow(loginException);

		assertThatThrownBy(() -> authGatewayController.login(request, servletRequest))
			.isSameAs(loginException);

		verify(authAccountPolicyService).markLoginFailure(request.getUsername());
		verify(authLoginAttemptService).record(eq(request.getUsername()), any(), eq(AuthLoginResult.FAILURE));
		verify(authAuditLogService).logPasswordLoginFailure(request.getUsername(), AuthFailureReason.INVALID_CREDENTIALS_OR_POLICY);
	}

	@Test
	void refreshReturnsRotatedTokenPair() {
		HttpServletRequest servletRequest = new MockHttpServletRequest();
		AuthTokens refreshed = new AuthTokens("new-access-token", "new-refresh-token");
		when(refreshTokenExtractor.extract(servletRequest)).thenReturn("old-refresh-token");
		when(authService.refresh("old-refresh-token")).thenReturn(refreshed);
		ResponseEntity<AuthResponse.TokenResponse> expected = ResponseEntity.ok(
			new AuthResponse.TokenResponse("new-access-token", "new-refresh-token")
		);
		when(authCookieResponseWriter.writeTokenCookies(any(AuthTokens.class), anyTokenResponseEntity())).thenReturn(expected);

		ResponseEntity<AuthResponse.TokenResponse> response = authGatewayController.refresh(servletRequest);

		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getAccessToken()).isEqualTo("new-access-token");
		assertThat(response.getBody().getRefreshToken()).isEqualTo("new-refresh-token");
		verify(authService).refresh("old-refresh-token");
	}

	@SuppressWarnings("unchecked")
	private ResponseEntity<AuthResponse.TokenResponse> anyTokenResponseEntity() {
		return (ResponseEntity<AuthResponse.TokenResponse>) any(ResponseEntity.class);
	}
}
