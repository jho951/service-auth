package com.authservice.app.security.platform.operational;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jho951.platform.governance.audit.LoggingAuditSink;
import org.junit.jupiter.api.Test;

class AuthPlatformOperationalConfigurationTest {

	@Test
	void usesPlatformLoggingAuditSinkForOperationalProfileFallback() {
		AuthPlatformOperationalConfiguration configuration = new AuthPlatformOperationalConfiguration();
		assertThat(configuration.authGovernanceAuditSink()).isInstanceOf(LoggingAuditSink.class);
	}
}
