package com.authservice.app.security.platform.governance;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jho951.platform.governance.api.PolicyConfigSource;
import io.github.jho951.platform.governance.config.MapPolicyConfigSource;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AuthPlatformGovernanceConfigurationTest {

	@Test
	void exposesPlatformMapPolicyConfigSource() {
		MockEnvironment environment = new MockEnvironment()
			.withProperty("platform.governance.policy-config.values[auth.audit.mode]", "strict");

		PolicyConfigSource source = new AuthPlatformGovernanceConfiguration()
			.authGovernancePolicyConfigSource(environment);

		assertThat(source).isInstanceOf(MapPolicyConfigSource.class);
		assertThat(source.resolve("auth.audit.mode")).contains("strict");
		assertThat(source.snapshot()).containsEntry("auth.audit.mode", "strict");
		assertThat(source.operationalStatus().isOperational()).isTrue();
	}

	@Test
	void dropsBlankKeysAndNullValuesFromPolicyConfigMap() {
		Map<String, String> sanitized = AuthPlatformGovernanceConfiguration.sanitizedPolicyConfigValues(
			Map.of(" valid.key ", "value", "   ", "ignored")
		);

		assertThat(sanitized).containsEntry("valid.key", "value");
		assertThat(sanitized).doesNotContainKey("   ");
	}

	@Test
	void reportsNotConfiguredWhenNoPolicyValuesAreDeclared() {
		PolicyConfigSource source = new AuthPlatformGovernanceConfiguration()
			.authGovernancePolicyConfigSource(new MockEnvironment());

		assertThat(source).isInstanceOf(MapPolicyConfigSource.class);
		assertThat(source.snapshot()).isEmpty();
		assertThat(source.operationalStatus().isOperational()).isFalse();
	}
}
