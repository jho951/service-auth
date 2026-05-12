package com.authservice.app.domain.auth.config;

import com.authservice.app.domain.auth.support.RefreshTokenExtractor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuthHttpProperties.class)
public class AuthHttpSupportConfig {

	@Bean
	public RefreshTokenExtractor refreshTokenExtractor(AuthCookieProperties properties) {
		return new RefreshTokenExtractor(properties);
	}
}
