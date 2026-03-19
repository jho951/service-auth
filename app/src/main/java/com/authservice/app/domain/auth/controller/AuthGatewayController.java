package com.authservice.app.domain.auth.controller;

import com.auth.api.model.Tokens;
import com.auth.config.controller.RefreshCookieWriter;
import com.auth.config.controller.RefreshTokenExtractor;
import com.auth.config.dto.LoginResponse;
import com.auth.core.service.AuthService;
import com.authservice.app.domain.auth.dto.AuthRequest;
import com.authservice.app.domain.auth.dto.AuthResponse;
import com.authservice.app.domain.auth.entity.Auth;
import com.authservice.app.domain.auth.service.AuthAccountPolicyService;
import com.authservice.app.domain.auth.service.AuthAuditService;
import com.authservice.app.domain.auth.service.AuthLoginAttemptService;
import com.authservice.app.domain.auth.service.AuthRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthGatewayController {

	private final AuthService authService;
	private final RefreshTokenExtractor refreshTokenExtractor;
	private final RefreshCookieWriter refreshCookieWriter;
	private final AuthAccountPolicyService authAccountPolicyService;
	private final AuthAuditService authAuditService;
	private final AuthLoginAttemptService authLoginAttemptService;

	public AuthGatewayController(
		AuthService authService,
		RefreshTokenExtractor refreshTokenExtractor,
		RefreshCookieWriter refreshCookieWriter,
		AuthAccountPolicyService authAccountPolicyService,
		AuthAuditService authAuditService,
		AuthLoginAttemptService authLoginAttemptService
	) {
		this.authService = authService;
		this.refreshTokenExtractor = refreshTokenExtractor;
		this.refreshCookieWriter = refreshCookieWriter;
		this.authAccountPolicyService = authAccountPolicyService;
		this.authAuditService = authAuditService;
		this.authLoginAttemptService = authLoginAttemptService;
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse.TokenResponse> login(@Valid @RequestBody AuthRequest.LoginRequest req, HttpServletRequest request) {
		AuthRequestContext context = AuthRequestContext.from(request);
		try {
			Tokens tokens = authService.login(req.getUsername(), req.getPassword());
			Optional<Auth> auth = authAccountPolicyService.markLoginSuccess(req.getUsername());
			authLoginAttemptService.record(req.getUsername(), context, "SUCCESS");
			auth.ifPresent(value -> authAuditService.log("LOGIN_SUCCESS", "SUCCESS", value, context, "{\"channel\":\"gateway\"}"));
			ResponseEntity<LoginResponse> response = refreshCookieWriter.write(
				tokens,
				ResponseEntity.ok(new LoginResponse(tokens.getAccessToken()))
			);
			return ResponseEntity.status(response.getStatusCode())
				.headers(response.getHeaders())
				.body(new AuthResponse.TokenResponse(tokens.getAccessToken(), tokens.getRefreshToken()));
		} catch (RuntimeException ex) {
			Optional<Auth> auth = authAccountPolicyService.markLoginFailure(req.getUsername());
			authLoginAttemptService.record(req.getUsername(), context, "FAILURE");
			auth.ifPresent(value -> authAuditService.log("LOGIN_FAILURE", "FAILURE", value, context, "{\"channel\":\"gateway\"}"));
			throw ex;
		}
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse.TokenResponse> refresh(HttpServletRequest request) {
		String refreshToken = refreshTokenExtractor.extract(request);
		Tokens tokens = authService.refresh(refreshToken);
		authAuditService.log("TOKEN_REFRESH", "SUCCESS", (Auth) null, AuthRequestContext.from(request), "{\"channel\":\"gateway\"}");
		ResponseEntity<LoginResponse> response = refreshCookieWriter.write(
			tokens,
			ResponseEntity.ok(new LoginResponse(tokens.getAccessToken()))
		);
		return ResponseEntity.status(response.getStatusCode())
			.headers(response.getHeaders())
			.body(new AuthResponse.TokenResponse(tokens.getAccessToken(), tokens.getRefreshToken()));
	}
}
