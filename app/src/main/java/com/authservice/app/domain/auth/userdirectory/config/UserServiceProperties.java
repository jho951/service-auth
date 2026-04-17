package com.authservice.app.domain.auth.userdirectory.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
	@Component
	@ConfigurationProperties(prefix = "user-service")
	public class UserServiceProperties {

		private String baseUrl;
		private Jwt jwt = new Jwt();

	@Getter
	@Setter
	public static class Jwt {
		private String issuer;
		private String audience;
		private String subject;
		private String scope;
		private long ttlSeconds = 60;
		private String secret;
	}
}
