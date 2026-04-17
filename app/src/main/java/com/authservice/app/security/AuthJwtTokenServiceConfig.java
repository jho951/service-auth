package com.authservice.app.security;

import com.authservice.app.domain.auth.config.AuthHttpProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AuthJwtTokenServiceConfig {

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
