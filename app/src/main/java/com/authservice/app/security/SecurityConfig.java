package com.authservice.app.security;

import com.authservice.app.domain.auth.sso.config.SsoProperties;
import com.authservice.app.domain.auth.sso.service.SsoOAuth2FailureHandler;
import com.authservice.app.domain.auth.sso.service.SsoOAuth2SuccessHandler;
import com.auth.config.security.AuthOncePerRequestFilter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

	private final AuthOncePerRequestFilter authFilter;
	private final RestAuthHandlers.EntryPoint entryPoint;
	private final RestAuthHandlers.Denied denied;
	private final SsoProperties ssoProperties;
	private final SsoOAuth2SuccessHandler ssoOAuth2SuccessHandler;
	private final SsoOAuth2FailureHandler ssoOAuth2FailureHandler;

	public SecurityConfig(AuthOncePerRequestFilter authFilter,
		RestAuthHandlers.EntryPoint entryPoint, RestAuthHandlers.Denied denied,
		SsoProperties ssoProperties,
		SsoOAuth2SuccessHandler ssoOAuth2SuccessHandler,
		SsoOAuth2FailureHandler ssoOAuth2FailureHandler) {
		this.authFilter = authFilter;
		this.entryPoint = entryPoint;
		this.denied = denied;
		this.ssoProperties = ssoProperties;
		this.ssoOAuth2SuccessHandler = ssoOAuth2SuccessHandler;
		this.ssoOAuth2FailureHandler = ssoOAuth2FailureHandler;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(cs -> cs.ignoringRequestMatchers(
				new AntPathRequestMatcher("/api/auth/**"),
				new AntPathRequestMatcher("/internal/auth/**"),
				new AntPathRequestMatcher("/auth/exchange", HttpMethod.POST.name()),
				new AntPathRequestMatcher("/auth/logout", HttpMethod.POST.name())
			))
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			.exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(denied))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/api/auth/**",
					"/internal/auth/**",
					"/auth/**",
					"/oauth2/**",
					"/login/oauth2/**",
					"/actuator/**",
					"/v3/api-docs/**",
					"/swagger-ui/**",
					"/swagger-ui.html",
					"/favicon.ico"
				).permitAll()
				.anyRequest().authenticated()
			)
			.httpBasic(basic -> basic.disable())
			.formLogin(form -> form.disable())
			.logout(logout -> logout.disable())
			.oauth2Login(oauth2 -> oauth2
				.successHandler(ssoOAuth2SuccessHandler)
				.failureHandler(ssoOAuth2FailureHandler)
			)
			.addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
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
