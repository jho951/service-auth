package com.authservice.app;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jho951.platform.security.api.SecurityAuditPublisher;
import io.github.jho951.platform.security.api.SecurityContextResolver;
import io.github.jho951.platform.security.auth.PlatformSessionSupport;
import io.github.jho951.platform.security.auth.TokenIssuanceCapability;
import io.github.jho951.platform.security.policy.PlatformSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(properties = {
	"SPRING_PROFILES_ACTIVE=dev",
	"AUTH_JWT_SECRET=test-auth-jwt-secret-test-auth-jwt-secret"
})
class AuthServiceApplicationTests {

	@Autowired
	private SecurityAuditPublisher securityAuditPublisher;

	@Autowired
	private SecurityContextResolver securityContextResolver;

	@Autowired
	private PlatformSecurityProperties platformSecurityProperties;

	@Autowired
	private TokenIssuanceCapability tokenIssuanceCapability;

	@Autowired
	private PlatformSessionSupport platformSessionSupport;

	@Test
	void wiresPlatformSecurityStarterBeans() {
		assertThat(securityAuditPublisher.getClass().getName())
			.isEqualTo("io.github.jho951.platform.security.governance.GovernanceSecurityAuditPublisher");
		assertThat(securityContextResolver).isNotNull();
		assertThat(tokenIssuanceCapability).isNotNull();
		assertThat(platformSessionSupport).isNotNull();
	}

	@Test
	void bindsPlatformSecurityIpGuardRules() {
		assertThat(platformSecurityProperties.getIpGuard().getAdmin().getRules())
			.contains("10.0.0.0/8");
		assertThat(platformSecurityProperties.getIpGuard().getInternal().getRules())
			.contains("172.16.0.0/12");
	}
}
