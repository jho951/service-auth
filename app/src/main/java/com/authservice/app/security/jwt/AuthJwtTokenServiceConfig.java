package com.authservice.app.security.jwt;

import com.authservice.app.domain.auth.config.AuthHttpProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * {@link AuthJwtTokenService} 빈 구성을 제공하는 설정 클래스입니다.
 */
@Configuration
public class AuthJwtTokenServiceConfig {

	/**
	 * 애플리케이션 설정값을 기반으로 JWT 토큰 서비스를 생성합니다.
	 *
	 * @param properties 인증 HTTP 및 JWT 속성
	 * @param audience 토큰 audience claim 값
	 * @return JWT 토큰 서비스 빈
	 */
	@Bean
	@Primary
	public AuthJwtTokenService tokenService(
		AuthHttpProperties properties,
		@Value("${AUTH_JWT_AUDIENCE:block-service}")
		String audience
	) {
		return new AuthJwtTokenService(
			properties.getJwt().getSecret(),
			audience,
			properties.getJwt().getAccessSeconds(),
			properties.getJwt().getRefreshSeconds()
		);
	}
}
