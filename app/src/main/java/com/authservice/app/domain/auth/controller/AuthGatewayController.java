package com.authservice.app.domain.auth.controller;

import com.authservice.common.logging.SensitiveDataMasker;
import com.authservice.app.domain.auth.dto.AuthRequest;
import com.authservice.app.domain.auth.dto.AuthResponse;
import com.authservice.app.domain.audit.service.AuthAuditLogService;
import com.authservice.app.domain.auth.service.AuthAccountPolicyService;
import com.authservice.app.domain.auth.service.AuthLoginAttemptService;
import com.authservice.app.domain.auth.model.AuthTokens;
import com.authservice.app.domain.auth.service.AuthLoginService;
import com.authservice.app.domain.auth.service.AuthRequestContext;
import com.authservice.app.domain.auth.sso.service.SsoCookieService;
import com.authservice.app.domain.auth.support.RefreshCookieWriter;
import com.authservice.app.domain.auth.support.RefreshTokenExtractor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 외부 망 또는 Gateway 계층에서 진입하는 인증 요청을 처리하는 컨트롤러입니다.
 * <p> 일반적인 인증 로직을 외부 클라이언트를 위한 엔드포인트(/auth)로 제공합니다.</p>
 */
@RestController
@RequestMapping("/auth")
public class AuthGatewayController {

	private static final Logger log = LoggerFactory.getLogger(AuthGatewayController.class);

	private final AuthLoginService authService;
	private final RefreshTokenExtractor refreshTokenExtractor;
	private final RefreshCookieWriter refreshCookieWriter;
	private final AuthAccountPolicyService authAccountPolicyService;
	private final AuthLoginAttemptService authLoginAttemptService;
	private final SsoCookieService ssoCookieService;
	private final AuthAuditLogService authAuditLogService;

	/**
	 * 생성자 생성
	 * @param authService              핵심 인증 및 토큰 발급 서비스
	 * @param refreshTokenExtractor    요청에서 리프레시 토큰을 추출하는 컴포넌트
	 * @param refreshCookieWriter      리프레시 토큰을 쿠키에 기록/삭제하는 컴포넌트
	 * @param authAccountPolicyService 계정 잠금 및 로그인 정책 관리 서비스
	 * @param authLoginAttemptService  로그인 시도 횟수 및 이력을 기록하는 서비스
	 */
	public AuthGatewayController(
		AuthLoginService authService,
		RefreshTokenExtractor refreshTokenExtractor,
		RefreshCookieWriter refreshCookieWriter,
		AuthAccountPolicyService authAccountPolicyService,
		AuthLoginAttemptService authLoginAttemptService,
		SsoCookieService ssoCookieService,
		AuthAuditLogService authAuditLogService
	) {
		this.authService = authService;
		this.refreshTokenExtractor = refreshTokenExtractor;
		this.refreshCookieWriter = refreshCookieWriter;
		this.authAccountPolicyService = authAccountPolicyService;
		this.authLoginAttemptService = authLoginAttemptService;
		this.ssoCookieService = ssoCookieService;
		this.authAuditLogService = authAuditLogService;
	}

	/**
	 * Gateway를 통한 로그인 요청을 처리합니다.
	 * @param req     로그인 요청 정보 (username, password)
	 * @param request 클라이언트 IP 및 컨텍스트 정보를 담은 객체
	 * @return 액세스 토큰 및 리프레시 토큰 응답
	 * @throws RuntimeException 인증 실패 시 발생
	 */
	@PostMapping("/login")
	public ResponseEntity<AuthResponse.TokenResponse> login(@Valid @RequestBody AuthRequest.LoginRequest req, HttpServletRequest request) {
		AuthRequestContext context = AuthRequestContext.from(request);
		try {
			AuthTokens tokens = authService.login(req.getUsername(), req.getPassword());
			authAccountPolicyService.markLoginSuccess(req.getUsername());
			authLoginAttemptService.record(req.getUsername(), context, "SUCCESS");
			authAuditLogService.logPasswordLoginSuccess(req.getUsername());

			ResponseEntity<Void> response = refreshCookieWriter.write(
				tokens,
				ResponseEntity.ok().build()
			);
			return ResponseEntity.status(response.getStatusCode())
				.headers(response.getHeaders())
				.header(HttpHeaders.SET_COOKIE, ssoCookieService.buildAccessTokenCookie(tokens.accessToken()))
				.body(new AuthResponse.TokenResponse(tokens.accessToken(), tokens.refreshToken()));
		} catch (RuntimeException ex) {
			authAccountPolicyService.markLoginFailure(req.getUsername());
			authLoginAttemptService.record(req.getUsername(), context, "FAILURE");
			log.warn("event=auth_login_request_failed username={} ip={} user_agent={} exception_type={}",
				SensitiveDataMasker.maskIdentifier(req.getUsername()),
				context.ip(),
				context.userAgent(),
				ex.getClass().getSimpleName());
			authAuditLogService.logPasswordLoginFailure(req.getUsername(), "INVALID_CREDENTIALS_OR_POLICY");
			throw ex;
		}
	}

	/**
	 * Gateway를 통한 토큰 갱신 요청을 처리합니다.
	 *
	 * @param request 리프레시 토큰 추출을 위한 객체
	 * @return 갱신된 토큰 세트
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
}
