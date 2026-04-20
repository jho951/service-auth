package com.authservice.common.swagger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile("!prod")
public class SwaggerConfig {

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("BackEnd API")
				.version("1.0")
				.description("API Documentation for Spring Boot App"))
			.addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
			.components(new Components()
				.addSecuritySchemes("bearerAuth",
					new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")
				))
			.servers(List.of(
				new Server().url("/").description("Current server")
			));
	}

	@Bean
	public GroupedOpenApi publicApi() {
		return GroupedOpenApi.builder()
			.group("Public APIs")
			.pathsToMatch("/api/**")
			.build();
	}

	@Bean
	public GroupedOpenApi authApi() {
		return GroupedOpenApi.builder()
			.group("Auth APIs")
			.pathsToMatch("/api/auth/**", "/auth/**")
			.build();
	}
}
