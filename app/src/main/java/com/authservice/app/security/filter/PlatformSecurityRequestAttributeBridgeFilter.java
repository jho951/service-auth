package com.authservice.app.security.filter;

import com.authservice.app.domain.auth.config.AuthCookieProperties;
import com.authservice.app.domain.auth.config.AuthHttpProperties;
import com.authservice.app.domain.auth.sso.config.SsoProperties;
import com.authservice.app.domain.auth.userdirectory.config.InternalApiProperties;
import com.authservice.app.security.platform.bridge.PlatformSecurityRequestAttributes;
import com.authservice.app.security.route.AuthRoutePolicy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * auth-service 요청을 platform-security가 이해할 수 있는 요청 속성으로 변환하는 브리지 필터입니다.
 */
@Component
public class PlatformSecurityRequestAttributeBridgeFilter extends OncePerRequestFilter {

	private final SsoProperties ssoProperties;
	private final AuthCookieProperties authCookieProperties;
	private final AuthHttpProperties authHttpProperties;
	private final AuthRoutePolicy authRoutePolicy;

	/**
	 * 플랫폼 보안 브리지에 필요한 쿠키, 헤더, 라우트 정책 의존성을 주입합니다.
	 *
	 * @param ssoProperties SSO 세션 쿠키 설정
	 * @param authCookieProperties 액세스 쿠키 설정
	 * @param authHttpProperties 인증 헤더 설정
	 * @param authRoutePolicy 경로별 보안 정책
	 */
	public PlatformSecurityRequestAttributeBridgeFilter(
		SsoProperties ssoProperties,
		AuthCookieProperties authCookieProperties,
		AuthHttpProperties authHttpProperties,
		AuthRoutePolicy authRoutePolicy
	) {
		this.ssoProperties = ssoProperties;
		this.authCookieProperties = authCookieProperties;
		this.authHttpProperties = authHttpProperties;
		this.authRoutePolicy = authRoutePolicy;
	}

	/**
	 * 쿠키와 헤더의 인증 단서를 platform-security 요청 속성으로 옮긴 뒤 다음 필터로 전달합니다.
	 *
	 * @param request 현재 HTTP 요청
	 * @param response 현재 HTTP 응답
	 * @param filterChain 다음 필터 체인
	 * @throws ServletException 서블릿 필터 처리 예외
	 * @throws IOException I/O 예외
	 */
	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		bridgeCookieCredentials(request);
		bridgeInternalCallerProof(request);
		filterChain.doFilter(request, response);
	}

	private void bridgeCookieCredentials(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return;
		}

		String sessionCookieName = ssoProperties.getSession().getCookieName();
		for (Cookie cookie : cookies) {
			if (cookie == null) {
				continue;
			}
			String name = cookie.getName();
			String value = cookie.getValue();
			if (value == null || value.isBlank()) {
				continue;
			}
			if (sessionCookieName.equals(name)) {
				request.setAttribute(PlatformSecurityRequestAttributes.SESSION_ID_ATTRIBUTE, value);
				continue;
			}
			if (authCookieProperties.accessCookie().name().equals(name)) {
				request.setAttribute(PlatformSecurityRequestAttributes.ACCESS_TOKEN_ATTRIBUTE, value);
			}
		}
	}

	private void bridgeInternalCallerProof(HttpServletRequest request) {
		if (!authRoutePolicy.isInternalPath(request)) {
			return;
		}

		String internalSecret = request.getHeader(InternalApiProperties.INTERNAL_SECRET_HEADER);
		if (internalSecret != null && !internalSecret.isBlank()) {
			request.setAttribute(
				PlatformSecurityRequestAttributes.INTERNAL_TOKEN_ATTRIBUTE,
				internalSecret
			);
			return;
		}

		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		String bearerPrefix = authHttpProperties.getBearerPrefix();
		if (
			authorization == null || authorization.isBlank() ||
			bearerPrefix == null || bearerPrefix.isBlank() ||
			!authorization.startsWith(bearerPrefix)
		) {
			return;
		}

		String token = authorization.substring(bearerPrefix.length()).trim();
		if (token.isBlank()) {
			return;
		}
		request.setAttribute(
			PlatformSecurityRequestAttributes.INTERNAL_TOKEN_ATTRIBUTE,
			token
		);
	}
}
