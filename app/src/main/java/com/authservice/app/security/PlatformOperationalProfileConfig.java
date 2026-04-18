package com.authservice.app.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlatformOperationalProfileConfig {

	@Bean
	public io.github.jho951.platform.policy.api.OperationalProfileResolver platformSecurityOperationalProfileResolver() {
		return io.github.jho951.platform.policy.api.OperationalProfileResolver.standard();
	}

	@Bean
	public io.github.jho951.platform.governance.api.OperationalProfileResolver platformGovernanceOperationalProfileResolver() {
		return io.github.jho951.platform.governance.api.OperationalProfileResolver.standard();
	}
}
