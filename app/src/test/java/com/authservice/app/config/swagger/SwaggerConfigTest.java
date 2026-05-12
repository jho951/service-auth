package com.authservice.app.config.swagger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

class SwaggerConfigTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(SwaggerConfig.class);

	@Test
	void doesNotRegisterSwaggerBeansWhenApiDocsDisabled() {
		contextRunner
			.withPropertyValues("springdoc.api-docs.enabled=false")
			.run(context -> {
				assertFalse(context.containsBean("customOpenAPI"));
				assertFalse(context.containsBean("publicApi"));
				assertFalse(context.containsBean("authApi"));
			});
	}

	@Test
	void registersSwaggerBeansWhenApiDocsEnabled() {
		contextRunner
			.withPropertyValues("springdoc.api-docs.enabled=true")
			.run(context -> {
				assertTrue(context.containsBean("customOpenAPI"));
				assertTrue(context.containsBean("publicApi"));
				assertTrue(context.containsBean("authApi"));
			});
	}

	@Test
	void customOpenApiContainsExpectedMetadataAndSecurityScheme() {
		contextRunner
			.withPropertyValues("springdoc.api-docs.enabled=true")
			.run(context -> {
				OpenAPI openAPI = context.getBean(OpenAPI.class);

				assertEquals("BackEnd API", openAPI.getInfo().getTitle());
				assertEquals("1.0", openAPI.getInfo().getVersion());
				assertEquals("API Documentation for Spring Boot App", openAPI.getInfo().getDescription());
				assertNotNull(openAPI.getServers());
				assertEquals(1, openAPI.getServers().size());
				assertEquals("/", openAPI.getServers().get(0).getUrl());

				Map<String, SecurityScheme> schemes = openAPI.getComponents().getSecuritySchemes();
				assertNotNull(schemes);
				assertTrue(schemes.containsKey("bearerAuth"));
				assertEquals(SecurityScheme.Type.HTTP, schemes.get("bearerAuth").getType());
				assertEquals("bearer", schemes.get("bearerAuth").getScheme());
				assertEquals("JWT", schemes.get("bearerAuth").getBearerFormat());
			});
	}

	@Test
	void groupedApisExposeExpectedGroupNames() {
		contextRunner
			.withPropertyValues("springdoc.api-docs.enabled=true")
			.run(context -> {
				GroupedOpenApi publicApi = context.getBean("publicApi", GroupedOpenApi.class);
				GroupedOpenApi authApi = context.getBean("authApi", GroupedOpenApi.class);

				assertEquals("Public APIs", publicApi.getGroup());
				assertEquals("Auth APIs", authApi.getGroup());
			});
	}
}
