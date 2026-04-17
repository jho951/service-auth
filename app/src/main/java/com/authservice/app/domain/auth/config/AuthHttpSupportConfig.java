package com.authservice.app.domain.auth.config;

import com.authservice.app.domain.auth.support.RefreshCookieWriter;
import com.authservice.app.domain.auth.support.RefreshTokenExtractor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuthHttpProperties.class)
public class AuthHttpSupportConfig {

	@Bean
	public RefreshCookieWriter refreshCookieWriter(AuthHttpProperties properties) {
		return new RefreshCookieWriter(properties);
	}

	@Bean
	public RefreshTokenExtractor refreshTokenExtractor(AuthHttpProperties properties) {
		return new RefreshTokenExtractor(properties);
	}

}
