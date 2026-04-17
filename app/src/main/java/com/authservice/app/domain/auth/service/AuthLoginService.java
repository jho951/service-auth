package com.authservice.app.domain.auth.service;

import com.authservice.app.common.base.constant.ErrorCode;
import com.authservice.app.common.base.exception.GlobalException;
import com.authservice.app.common.logging.SensitiveDataMasker;
import com.authservice.app.domain.auth.config.AuthHttpProperties;
import com.authservice.app.domain.auth.model.AuthPrincipal;
import com.authservice.app.domain.auth.model.AuthTokens;
import com.authservice.app.security.AuthJwtTokenService;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuthLoginService {

	private static final Logger log = LoggerFactory.getLogger(AuthLoginService.class);

	private final AuthUserFinder userFinder;
	private final AuthPasswordVerifier passwordVerifier;
	private final AuthJwtTokenService tokenService;
	private final AuthRedisRefreshTokenStore refreshTokenStore;
	private final Duration refreshTtl;

	public AuthLoginService(
		AuthUserFinder userFinder,
		AuthPasswordVerifier passwordVerifier,
		AuthJwtTokenService tokenService,
		AuthRedisRefreshTokenStore refreshTokenStore,
		AuthHttpProperties properties
	) {
		this.userFinder = userFinder;
		this.passwordVerifier = passwordVerifier;
		this.tokenService = tokenService;
		this.refreshTokenStore = refreshTokenStore;
		this.refreshTtl = Duration.ofSeconds(properties.getJwt().getRefreshSeconds());
	}

	public AuthTokens login(String username, String password) {
		var user = userFinder.findByUsername(username).orElseThrow(() -> {
			log.warn("event=auth_login_failed reason=user_not_found username={}",
				SensitiveDataMasker.maskIdentifier(username));
			return new GlobalException(ErrorCode.UNAUTHORIZED);
		});
		if (!passwordVerifier.matches(password, user.passwordHash())) {
			log.warn("event=auth_login_failed reason=password_mismatch user_id={} username={}",
				user.userId(),
				SensitiveDataMasker.maskIdentifier(username));
			throw new GlobalException(ErrorCode.UNAUTHORIZED);
		}
		AuthPrincipal principal = new AuthPrincipal(
			user.userId(),
			user.roles(),
			Map.of("username", user.username())
		);
		AuthTokens tokens = issue(principal);
		refreshTokenStore.save(principal.userId(), tokens.refreshToken(), Instant.now().plus(refreshTtl));
		log.info("event=auth_login_succeeded user_id={}", principal.userId());
		return tokens;
	}

	public AuthTokens refresh(String refreshToken) {
		AuthPrincipal principal = tokenService.verifyRefreshToken(refreshToken);
		if (!refreshTokenStore.exists(principal.userId(), refreshToken)) {
			log.warn("event=auth_refresh_failed reason=refresh_token_not_found user_id={}", principal.userId());
			throw new GlobalException(ErrorCode.INVALID_TOKEN);
		}
		refreshTokenStore.revoke(principal.userId(), refreshToken);
		AuthTokens tokens = issue(principal);
		refreshTokenStore.save(principal.userId(), tokens.refreshToken(), Instant.now().plus(refreshTtl));
		log.info("event=auth_refresh_succeeded user_id={}", principal.userId());
		return tokens;
	}

	public void logout(String refreshToken) {
		AuthPrincipal principal = tokenService.verifyRefreshToken(refreshToken);
		refreshTokenStore.revoke(principal.userId(), refreshToken);
		log.info("event=auth_logout_succeeded user_id={}", principal.userId());
	}

	private AuthTokens issue(AuthPrincipal principal) {
		return new AuthTokens(
			tokenService.issueAccessToken(principal),
			tokenService.issueRefreshToken(principal)
		);
	}
}
