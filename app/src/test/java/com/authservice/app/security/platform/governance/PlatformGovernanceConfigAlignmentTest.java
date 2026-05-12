package com.authservice.app.security.platform.governance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

class PlatformGovernanceConfigAlignmentTest {

	@Test
	void governanceUsesIdentityServicePreset() {
		assertThat(yamlProperties().getProperty("platform.governance.service-role-preset"))
			.contains("IDENTITY_SERVICE");
	}

	@Test
	void governanceDoesNotDeclareObsoletePolicyEngineBackendKeys() {
		Properties properties = yamlProperties();

		assertThat(properties.getProperty("platform.governance.policy-config.store")).isNull();
		assertThat(properties.getProperty("platform.governance.policy-config.file-path")).isNull();
		assertThat(properties.getProperty("platform.governance.policy-config.cache-ttl-millis")).isNull();
	}

	private static Properties yamlProperties() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ClassPathResource("application.yml"));
		Properties properties = factory.getObject();
		assertThat(properties).isNotNull();
		return properties;
	}
}
