package com.authservice.app.security;

import com.authservice.app.domain.auth.sso.config.SsoProperties;
import com.authservice.app.domain.auth.sso.service.SsoOAuth2FailureHandler;
import com.authservice.app.domain.auth.sso.service.SsoOAuth2SuccessHandler;
import jakarta.servlet.Filter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private final RestAuthHandlers.EntryPoint entryPoint;
	private final RestAuthHandlers.Denied denied;
	private final SsoProperties ssoProperties;
	private final SsoOAuth2SuccessHandler ssoOAuth2SuccessHandler;
	private final SsoOAuth2FailureHandler ssoOAuth2FailureHandler;
	private final Filter platformSecurityServletFilter;
	private final Environment environment;

	public SecurityConfig(
		RestAuthHandlers.EntryPoint entryPoint,
		RestAuthHandlers.Denied denied,
		SsoProperties ssoProperties,
		SsoOAuth2SuccessHandler ssoOAuth2SuccessHandler,
		SsoOAuth2FailureHandler ssoOAuth2FailureHandler,
		@Qualifier("securityServletFilter") Filter platformSecurityServletFilter,
		Environment environment) {
		this.entryPoint = entryPoint;
		this.denied = denied;
		this.ssoProperties = ssoProperties;
		this.ssoOAuth2SuccessHandler = ssoOAuth2SuccessHandler;
		this.ssoOAuth2FailureHandler = ssoOAuth2FailureHandler;
		this.platformSecurityServletFilter = platformSecurityServletFilter;
		this.environment = environment;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(cs -> cs.ignoringRequestMatchers(
				new AntPathRequestMatcher("/internal/auth/**"),
				new AntPathRequestMatcher("/auth/login", HttpMethod.POST.name()),
				new AntPathRequestMatcher("/auth/refresh", HttpMethod.POST.name()),
				new AntPathRequestMatcher("/auth/logout", HttpMethod.POST.name()),
				new AntPathRequestMatcher("/auth/internal/session/validate", HttpMethod.POST.name()),
				new AntPathRequestMatcher("/auth/exchange", HttpMethod.POST.name())
			))
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			.exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(denied))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/auth/session"
				).authenticated()
				.requestMatchers(
					publicRequestMatchers()
				).permitAll()
				.anyRequest().authenticated()
			)
			.httpBasic(basic -> basic.disable())
			.formLogin(form -> form.disable())
			.logout(logout -> logout.disable())
			.oauth2Login(oauth2 -> oauth2
				.authorizationEndpoint(authorization -> authorization
					.baseUri("/oauth2/authorization")
				)
				.redirectionEndpoint(redirection -> redirection
					.baseUri("/login/oauth2/code/*")
				)
				.successHandler(ssoOAuth2SuccessHandler)
				.failureHandler(ssoOAuth2FailureHandler)
			)
			.addFilterBefore(platformSecurityServletFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	private String[] publicRequestMatchers() {
		List<String> matchers = new ArrayList<>(List.of(
			"/",
			"/.well-known/**",
			"/error",
			"/internal/auth/**",
			"/auth/login",
			"/auth/refresh",
			"/auth/logout",
			"/auth/sso/start",
			"/auth/oauth/github/callback",
			"/auth/login/github",
			"/auth/oauth2/authorize/github",
			"/auth/exchange",
			"/auth/me",
			"/auth/internal/session/validate",
			"/oauth2/**",
			"/login/oauth2/**",
			"/actuator/health",
			"/actuator/health/**",
			"/favicon.ico"
		));

		if (environment.acceptsProfiles(Profiles.of("dev", "dev_docs"))) {
			matchers.addAll(List.of(
				"/v3/api-docs/**",
				"/swagger-ui/**",
				"/swagger-ui.html"
			));
		}

		return matchers.toArray(String[]::new);
	}

	@Bean
	public FilterRegistrationBean<Filter> platformSecurityServletFilterRegistration(
		@Qualifier("securityServletFilter") Filter platformSecurityServletFilter
	) {
		FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(platformSecurityServletFilter);
		registration.setEnabled(false);
		return registration;
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(ssoProperties.getFrontend().getAllowedOrigins());
		configuration.setAllowedMethods(List.of(
			HttpMethod.GET.name(),
			HttpMethod.POST.name(),
			HttpMethod.OPTIONS.name()
		));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
