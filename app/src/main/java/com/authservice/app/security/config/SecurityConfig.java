package com.authservice.app.security.config;

import com.authservice.app.domain.auth.support.AuthPrincipalNames;
import com.authservice.app.domain.auth.sso.config.SsoProperties;
import com.authservice.app.domain.auth.sso.service.SsoOAuth2FailureHandler;
import com.authservice.app.domain.auth.sso.service.SsoOAuth2SuccessHandler;
import com.authservice.app.security.filter.CookieCsrfOriginGuardFilter;
import com.authservice.app.security.filter.PlatformSecurityRequestAttributeBridgeFilter;
import com.authservice.app.security.route.AuthRoutePolicy;
import com.authservice.common.security.RestAuthHandlers;
import jakarta.servlet.Filter;
import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * auth-service의 메인 Spring Security 필터 체인을 구성하는 설정 클래스입니다.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private final RestAuthHandlers.EntryPoint entryPoint;
	private final RestAuthHandlers.Denied denied;
	private final SsoProperties ssoProperties;
	private final SsoOAuth2SuccessHandler ssoOAuth2SuccessHandler;
	private final SsoOAuth2FailureHandler ssoOAuth2FailureHandler;
	private final Filter platformSecurityServletFilter;
	private final PlatformSecurityRequestAttributeBridgeFilter platformSecurityRequestAttributeBridgeFilter;
	private final CookieCsrfOriginGuardFilter cookieCsrfOriginGuardFilter;
	private final Environment environment;
	private final AuthRoutePolicy authRoutePolicy;

	/**
	 * 메인 보안 필터 체인 구성에 필요한 인증/인가 협력 객체를 주입합니다.
	 *
	 * @param entryPoint 인증 실패 응답 처리기
	 * @param denied 인가 실패 응답 처리기
	 * @param ssoProperties SSO 프런트엔드 설정
	 * @param ssoOAuth2SuccessHandler OAuth2 로그인 성공 처리기
	 * @param ssoOAuth2FailureHandler OAuth2 로그인 실패 처리기
	 * @param platformSecurityServletFilter platform-security 서블릿 필터
	 * @param platformSecurityRequestAttributeBridgeFilter platform-security 요청 속성 브리지 필터
	 * @param cookieCsrfOriginGuardFilter 쿠키 기반 CSRF 원본 검증 필터
	 * @param environment 환경 속성 조회기
	 * @param authRoutePolicy 경로별 보안 정책
	 */
	public SecurityConfig(
		RestAuthHandlers.EntryPoint entryPoint,
		RestAuthHandlers.Denied denied,
		SsoProperties ssoProperties,
		SsoOAuth2SuccessHandler ssoOAuth2SuccessHandler,
		SsoOAuth2FailureHandler ssoOAuth2FailureHandler,
		@Qualifier("securityServletFilter") Filter platformSecurityServletFilter,
		PlatformSecurityRequestAttributeBridgeFilter platformSecurityRequestAttributeBridgeFilter,
		CookieCsrfOriginGuardFilter cookieCsrfOriginGuardFilter,
		Environment environment,
		AuthRoutePolicy authRoutePolicy) {
		this.entryPoint = entryPoint;
		this.denied = denied;
		this.ssoProperties = ssoProperties;
		this.ssoOAuth2SuccessHandler = ssoOAuth2SuccessHandler;
		this.ssoOAuth2FailureHandler = ssoOAuth2FailureHandler;
		this.platformSecurityServletFilter = platformSecurityServletFilter;
		this.platformSecurityRequestAttributeBridgeFilter = platformSecurityRequestAttributeBridgeFilter;
		this.cookieCsrfOriginGuardFilter = cookieCsrfOriginGuardFilter;
		this.environment = environment;
		this.authRoutePolicy = authRoutePolicy;
	}

	/**
	 * 보안 필터 체인과 경로별 인증 정책을 구성합니다.
	 *
	 * @param http Spring Security HTTP 보안 빌더
	 * @return 구성된 보안 필터 체인
	 * @throws Exception 보안 구성 도중 발생할 수 있는 예외
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(cs -> cs.ignoringRequestMatchers(authRoutePolicy.csrfIgnoredMatchers()))
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			.exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(denied))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					internalPassThroughRequestMatchers()
				).hasAuthority(AuthPrincipalNames.INTERNAL_ROLE)
				.requestMatchers(
					adminRequestMatchers()
				).authenticated()
				.requestMatchers(
					protectedRequestMatchers()
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
			.addFilterBefore(platformSecurityRequestAttributeBridgeFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(cookieCsrfOriginGuardFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(platformSecurityServletFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	private String[] protectedRequestMatchers() {
		return authRoutePolicy.protectedRequestMatchers();
	}

	private String[] adminRequestMatchers() {
		return authRoutePolicy.adminRequestMatchers();
	}

	private String[] publicRequestMatchers() {
		return authRoutePolicy.publicRequestMatchers(isDocsEnabled());
	}

	private boolean isDocsEnabled() {
		return environment.getProperty("springdoc.api-docs.enabled", Boolean.class, false);
	}

	private String[] internalPassThroughRequestMatchers() {
		return authRoutePolicy.internalPassThroughRequestMatchers();
	}

	/**
	 * 서블릿 컨테이너의 자동 등록을 비활성화한 CSRF 원본 검증 필터 등록을 제공합니다.
	 *
	 * @param cookieCsrfOriginGuardFilter 수동으로 필터 체인에 삽입할 CSRF 원본 검증 필터
	 * @return 자동 등록이 비활성화된 필터 등록 빈
	 */
	@Bean
	public FilterRegistrationBean<CookieCsrfOriginGuardFilter> cookieCsrfOriginGuardFilterRegistration(
		CookieCsrfOriginGuardFilter cookieCsrfOriginGuardFilter
	) {
		FilterRegistrationBean<CookieCsrfOriginGuardFilter> registration = new FilterRegistrationBean<>(cookieCsrfOriginGuardFilter);
		registration.setEnabled(false);
		return registration;
	}

	/**
	 * SSO 프런트엔드와 통신할 CORS 정책을 구성합니다.
	 *
	 * @return auth-service용 CORS 설정 소스
	 */
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
