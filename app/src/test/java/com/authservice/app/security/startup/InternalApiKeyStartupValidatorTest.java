package com.authservice.app.security.startup;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.authservice.app.domain.auth.userdirectory.config.InternalApiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

class InternalApiKeyStartupValidatorTest {

	@Test
	void rejectsBlankInternalKeyInProd() {
		InternalApiProperties properties = new InternalApiProperties();
		Environment environment = environment("prod");
		InternalApiKeyStartupValidator validator = new InternalApiKeyStartupValidator(environment, properties);

		assertThatThrownBy(() -> validator.run(null))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("INTERNAL_API_KEY");
	}

	@Test
	void rejectsLocalInternalKeyInProd() {
		InternalApiProperties properties = new InternalApiProperties();
		properties.setKey("local-internal-api-key");
		Environment environment = environment("prod");
		InternalApiKeyStartupValidator validator = new InternalApiKeyStartupValidator(environment, properties);

		assertThatThrownBy(() -> validator.run(null))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("INTERNAL_API_KEY");
	}

	@Test
	void allowsExplicitInternalKeyInProd() {
		InternalApiProperties properties = new InternalApiProperties();
		properties.setKey("prod-secret");
		Environment environment = environment("prod");
		InternalApiKeyStartupValidator validator = new InternalApiKeyStartupValidator(environment, properties);

		assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
	}

	@Test
	void allowsLocalInternalKeyOutsideProd() {
		InternalApiProperties properties = new InternalApiProperties();
		properties.setKey("local-internal-api-key");
		Environment environment = environment("dev");
		InternalApiKeyStartupValidator validator = new InternalApiKeyStartupValidator(environment, properties);

		assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
	}

	private Environment environment(String... activeProfiles) {
		Environment environment = org.mockito.Mockito.mock(Environment.class);
		when(environment.getActiveProfiles()).thenReturn(activeProfiles);
		return environment;
	}
}
