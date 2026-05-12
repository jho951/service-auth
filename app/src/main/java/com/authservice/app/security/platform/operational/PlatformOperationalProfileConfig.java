package com.authservice.app.security.platform.operational;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * platform-security와 platform-governance의 운영 프로필 resolver를 제공하는 설정입니다.
 */
@Configuration
public class PlatformOperationalProfileConfig {

	/**
	 * platform-security용 운영 프로필 resolver를 생성합니다.
	 *
	 * @return platform-security 운영 프로필 resolver
	 */
	@Bean
	public io.github.jho951.platform.policy.api.OperationalProfileResolver platformSecurityOperationalProfileResolver() {
		return io.github.jho951.platform.policy.api.OperationalProfileResolver.standard();
	}

	/**
	 * platform-governance용 운영 프로필 resolver를 생성합니다.
	 *
	 * @return platform-governance 운영 프로필 resolver
	 */
	@Bean
	public io.github.jho951.platform.governance.api.OperationalProfileResolver platformGovernanceOperationalProfileResolver() {
		return io.github.jho951.platform.governance.api.OperationalProfileResolver.standard();
	}
}
