package com.authservice.app;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jho951.platform.security.api.SecurityAuditPublisher;
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

	@Test
	void wiresSecurityAuditPublisherToGovernanceBridge() {
		assertThat(securityAuditPublisher.getClass().getName())
			.isEqualTo("io.github.jho951.platform.security.governance.GovernanceSecurityAuditPublisher");
	}
}
