package com.authservice.app.domain.auth.controller;

import com.authservice.app.domain.auth.entity.Auth;

import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.authservice.app.domain.auth.dto.AuthRequest;
import com.authservice.app.domain.auth.dto.AuthResponse;
import com.authservice.app.domain.auth.service.AuthRequestContext;
import com.authservice.app.domain.auth.model.AuthTokens;
import com.authservice.app.domain.auth.service.AuthLoginService;
import com.authservice.app.domain.auth.service.AuthLoginAttemptService;
import com.authservice.app.domain.auth.service.AuthAccountPolicyService;
import com.authservice.app.domain.auth.sso.service.SsoCookieService;
import com.authservice.app.domain.auth.support.RefreshCookieWriter;
import com.authservice.app.domain.auth.support.RefreshTokenExtractor;
import org.springframework.http.HttpHeaders;

/** 인증 및 인가 처리 담당 컨트롤러입니다. */
public class AuthController {

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	private final AuthLoginService authService;
	private final RefreshTokenExtractor refreshTokenExtractor;
	private final RefreshCookieWriter refreshCookieWriter;
	private final AuthAccountPolicyService authAccountPolicyService;
	private final AuthLoginAttemptService authLoginAttemptService;
	private final SsoCookieService ssoCookieService;

	/**
	 * 생성자
	 * @param authService              핵심 인증 로직 서비스
	 * @param refreshTokenExtractor    HTTP 요청에서 리프레시 토큰을 추출하는 컴포넌트
	 * @param refreshCookieWriter      리프레시 토큰을 쿠키에 기록하는 컴포넌트
	 * @param authAccountPolicyService 계정 잠금 및 로그인 정책 서비스
	 * @param authLoginAttemptService  로그인 시도 이력을 기록하는 서비스
	 */
	public AuthController(
		AuthLoginService authService,
		RefreshTokenExtractor refreshTokenExtractor,
		RefreshCookieWriter refreshCookieWriter,
		AuthAccountPolicyService authAccountPolicyService,
		AuthLoginAttemptService authLoginAttemptService,
		SsoCookieService ssoCookieService
	) {
		this.authService = authService;
		this.refreshTokenExtractor = refreshTokenExtractor;
		this.refreshCookieWriter = refreshCookieWriter;
		this.authAccountPolicyService = authAccountPolicyService;
		this.authLoginAttemptService = authLoginAttemptService;
		this.ssoCookieService = ssoCookieService;
	}

	/**
	 * 사용자 자격 증명(ID/PW)을 확인하여 로그인을 수행합니다.
	 * <p>
	 * 성공 시 액세스 토큰과 리프레시 토큰을 발급하며, 실패 시 실패 횟수를 기록하고 예외를 던집니다.
	 * </p>
	 * @param req     사용자 아이디와 비밀번호를 담은 요청 객체
	 * @param request 클라이언트 정보를 추출하기 위한 HttpServletRequest
	 * @return ResponseEntity 발급된 토큰 정보를 포함
	 * @throws RuntimeException 인증 실패 또는 계정 정책에 위반될 경우 발생
	 */
	@PostMapping("/login")
	public ResponseEntity<AuthResponse.TokenResponse> login(@Valid @RequestBody AuthRequest.LoginRequest req, HttpServletRequest request) {
		AuthRequestContext context = AuthRequestContext.from(request);
		try {
			AuthTokens tokens = authService.login(req.getUsername(), req.getPassword());
			Optional<Auth> auth = authAccountPolicyService.markLoginSuccess(req.getUsername());
			authLoginAttemptService.record(req.getUsername(), context, "SUCCESS");

			ResponseEntity<Void> response = refreshCookieWriter.write(
				tokens,
				ResponseEntity.ok().build()
			);

			return ResponseEntity.status(response.getStatusCode())
				.headers(response.getHeaders())
				.header(HttpHeaders.SET_COOKIE, ssoCookieService.buildAccessTokenCookie(tokens.accessToken()))
				.body(new AuthResponse.TokenResponse(tokens.accessToken(), tokens.refreshToken()));

		} catch (RuntimeException ex) {
			Optional<Auth> auth = authAccountPolicyService.markLoginFailure(req.getUsername());
			authLoginAttemptService.record(req.getUsername(), context, "FAILURE");
			log.warn("Login failed. method=POST uri=/login username={} ip={} userAgent={} exceptionType={} message={}",
				req.getUsername(),
				context.ip(),
				context.userAgent(),
				ex.getClass().getSimpleName(),
				ex.getMessage());
			throw ex;
		}
	}

	/**
	 * 리프레시 토큰을 사용하여 새로운 액세스 토큰 및 리프레시 토큰을 갱신합니다.
	 *
	 * @param request 쿠키 또는 헤더에서 리프레시 토큰을 추출하기 위한 HttpServletRequest
	 * @return ResponseEntity 갱신된 토큰 정보를 포함
	 */
	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse.TokenResponse> refresh(HttpServletRequest request) {
		String refreshToken = refreshTokenExtractor.extract(request);
		AuthTokens tokens = authService.refresh(refreshToken);

		ResponseEntity<Void> response = refreshCookieWriter.write(
			tokens,
			ResponseEntity.ok().build()
		);

		return ResponseEntity.status(response.getStatusCode())
			.headers(response.getHeaders())
			.header(HttpHeaders.SET_COOKIE, ssoCookieService.buildAccessTokenCookie(tokens.accessToken()))
			.body(new AuthResponse.TokenResponse(tokens.accessToken(), tokens.refreshToken()));
	}

	/**
	 * 사용자를 로그아웃 처리하고 세션(리프레시 토큰)을 무효화합니다.
	 * <p> 클라이언트 쿠키에 저장된 리프레시 토큰을 삭제하도록 지시합니다. </p>
	 *
	 * @param request 리프레시 토큰 추출을 위한 HttpServletRequest
	 * @return HTTP 204 No Content 및 쿠키 삭제 헤더
	 */
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		String refreshToken = refreshTokenExtractor.extract(request);
		authService.logout(refreshToken);
		ResponseEntity<Void> response = refreshCookieWriter.clear(ResponseEntity.noContent().build());
		return ResponseEntity.status(response.getStatusCode())
			.headers(response.getHeaders())
			.header(HttpHeaders.SET_COOKIE, ssoCookieService.clearAccessTokenCookie())
			.build();
	}
}
