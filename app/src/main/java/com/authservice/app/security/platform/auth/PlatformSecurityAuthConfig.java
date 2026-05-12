package com.authservice.app.security.platform.auth;

import com.authservice.app.domain.auth.support.AuthPrincipalNames;
import com.authservice.app.domain.auth.userdirectory.config.InternalApiProperties;
import com.authservice.app.security.platform.bridge.PlatformSecurityRequestAttributes;
import io.github.jho951.platform.security.api.SecurityContextResolver;
import io.github.jho951.platform.security.auth.DefaultAuthenticationCapabilityResolver;
import io.github.jho951.platform.security.auth.DefaultHybridAuthenticationCapability;
import io.github.jho951.platform.security.auth.DefaultInternalServiceAuthenticationCapability;
import io.github.jho951.platform.security.auth.DefaultJwtAuthenticationCapability;
import io.github.jho951.platform.security.auth.DefaultSessionAuthenticationCapability;
import io.github.jho951.platform.security.auth.InternalServiceCompatibilityAuthenticationAdapter;
import io.github.jho951.platform.security.auth.InternalTokenClaimsValidator;
import io.github.jho951.platform.security.auth.PlatformAuthenticatedPrincipal;
import io.github.jho951.platform.security.auth.PlatformAuthenticationFacade;
import io.github.jho951.platform.security.auth.PlatformSessionSupport;
import io.github.jho951.platform.security.auth.PlatformSessionSupportFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * auth-service의 platform-security 인증 해석 전략을 구성하는 설정입니다.
 */
@Configuration
public class PlatformSecurityAuthConfig {

	/**
	 * 내부 경계 요청에서 내부 서비스 principal만 허용하는 claims validator를 제공합니다.
	 *
	 * @return 내부 서비스 전용 claims validator
	 */
	@Bean(name = "platformSecurityLocalInternalTokenClaimsValidator")
	public InternalTokenClaimsValidator platformSecurityLocalInternalTokenClaimsValidator() {
		return (principal, request) -> isInternalBoundary(request.attributes()) && isInternalPrincipal(principal);
	}

	/**
	 * 내부 API 키를 platform-security 내부 호출 principal로 변환하는 호환 어댑터를 제공합니다.
	 *
	 * @param internalApiProperties 내부 API 키 설정
	 * @return 내부 서비스 인증 어댑터
	 */
	@Bean
	public InternalServiceCompatibilityAuthenticationAdapter internalServiceCompatibilityAuthenticationAdapter(
		InternalApiProperties internalApiProperties
	) {
		return request -> {
			if (!isInternalBoundary(request.attributes())) {
				return Optional.empty();
			}

			String internalToken = request.attributes().get(PlatformSecurityRequestAttributes.INTERNAL_TOKEN_ATTRIBUTE);
			if (internalToken == null || internalToken.isBlank()) {
				return Optional.empty();
			}

			try {
				internalApiProperties.validateInternalToken(internalToken);
				return Optional.of(internalPrincipal());
			} catch (RuntimeException ex) {
				return Optional.empty();
			}
		};
	}

	/**
	 * platform-security가 사용할 메인 {@link SecurityContextResolver}를 구성합니다.
	 *
	 * @param platformSessionSupportFactory 세션 지원 팩토리
	 * @param platformSecurityLocalInternalTokenClaimsValidator 내부 호출 claims validator
	 * @param internalServiceCompatibilityAuthenticationAdapter 내부 호출 호환 어댑터
	 * @return 보안 컨텍스트 resolver
	 */
	@Bean
	@Primary
	public SecurityContextResolver securityContextResolver(
		PlatformSessionSupportFactory platformSessionSupportFactory,
		InternalTokenClaimsValidator platformSecurityLocalInternalTokenClaimsValidator,
		InternalServiceCompatibilityAuthenticationAdapter internalServiceCompatibilityAuthenticationAdapter
	) {
		PlatformSessionSupport platformSessionSupport = platformSessionSupportFactory.create();
		return new PlatformAuthenticationFacade(
			new DefaultAuthenticationCapabilityResolver(
				new DefaultJwtAuthenticationCapability(platformSessionSupport),
				new DefaultSessionAuthenticationCapability(platformSessionSupport),
				new DefaultHybridAuthenticationCapability(platformSessionSupport),
				new DefaultInternalServiceAuthenticationCapability(
					platformSessionSupport,
					platformSecurityLocalInternalTokenClaimsValidator,
					List.of(internalServiceCompatibilityAuthenticationAdapter)
				)
			)
		);
	}

	private static boolean isInternalBoundary(Map<String, String> attributes) {
		String boundary = attributes.get(PlatformSecurityRequestAttributes.SECURITY_BOUNDARY_ATTRIBUTE);
		return boundary != null && "INTERNAL".equalsIgnoreCase(boundary.trim());
	}

	private static boolean isInternalPrincipal(PlatformAuthenticatedPrincipal principal) {
		return AuthPrincipalNames.INTERNAL_SERVICE.equals(principal.userId())
			&& principal.authorities().contains(AuthPrincipalNames.INTERNAL_ROLE)
			&& "internal".equals(String.valueOf(principal.attributes().get("authType")));
	}

	private static PlatformAuthenticatedPrincipal internalPrincipal() {
		return new PlatformAuthenticatedPrincipal(
			AuthPrincipalNames.INTERNAL_SERVICE,
			java.util.Set.of(AuthPrincipalNames.INTERNAL_ROLE),
			Map.of("authType", "internal")
		);
	}
}
