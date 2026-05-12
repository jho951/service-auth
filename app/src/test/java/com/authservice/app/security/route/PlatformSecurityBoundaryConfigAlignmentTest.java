package com.authservice.app.security.route;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

class PlatformSecurityBoundaryConfigAlignmentTest {

	private final AuthRoutePolicy authRoutePolicy = new AuthRoutePolicy();

	@Test
	void publicBoundaryConfigMatchesRoutePolicy() {
		assertThat(indexedValues("platform.security.boundary.public-paths"))
			.containsExactly(authRoutePolicy.publicRequestMatchers(false));
	}

	@Test
	void protectedBoundaryConfigMatchesRoutePolicy() {
		assertThat(indexedValues("platform.security.boundary.protected-paths"))
			.containsExactly(authRoutePolicy.protectedRequestMatchers());
	}

	@Test
	void adminBoundaryConfigMatchesRoutePolicy() {
		assertThat(indexedValues("platform.security.boundary.admin-paths"))
			.containsExactly(authRoutePolicy.adminRequestMatchers());
	}

	@Test
	void internalBoundaryConfigMatchesRoutePolicy() {
		assertThat(indexedValues("platform.security.boundary.internal-paths"))
			.containsExactly(authRoutePolicy.internalPassThroughRequestMatchers());
	}

	private static List<String> indexedValues(String prefix) {
		Properties properties = yamlProperties();
		List<String> values = new ArrayList<>();
		for (int index = 0; ; index++) {
			String value = properties.getProperty(prefix + "[" + index + "]");
			if (value == null) {
				return values;
			}
			values.add(value);
		}
	}

	private static Properties yamlProperties() {
		YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
		factory.setResources(new ClassPathResource("application.yml"));
		Properties properties = factory.getObject();
		assertThat(properties).isNotNull();
		return properties;
	}
}
