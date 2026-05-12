package com.authservice.app.security.filter;

import com.authservice.common.base.constant.ErrorCode;
import com.authservice.common.base.dto.GlobalResponse;
import com.authservice.app.domain.auth.config.AuthCookieProperties;
import com.authservice.app.domain.auth.sso.config.SsoProperties;
import com.authservice.app.security.route.AuthRoutePolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 쿠키 기반 인증 요청에 대해 허용된 Origin/Referer인지 검증하는 CSRF 방어 필터입니다.
 */
@Component
public class CookieCsrfOriginGuardFilter extends OncePerRequestFilter {

	private final SsoProperties ssoProperties;
	private final AuthCookieProperties authCookieProperties;
	private final AuthRoutePolicy authRoutePolicy;
	private final ObjectMapper objectMapper;

	/**
	 * 쿠키 기반 CSRF 원본 검증에 필요한 설정과 협력 객체를 주입합니다.
	 *
	 * @param ssoProperties SSO 세션 및 프런트엔드 허용 Origin 설정
	 * @param authCookieProperties 액세스/리프레시 쿠키 설정
	 * @param authRoutePolicy 경로별 보안 정책
	 * @param objectMapper 실패 응답 직렬화기
	 */
	public CookieCsrfOriginGuardFilter(
		SsoProperties ssoProperties,
		AuthCookieProperties authCookieProperties,
		AuthRoutePolicy authRoutePolicy,
		ObjectMapper objectMapper
	) {
		this.ssoProperties = ssoProperties;
		this.authCookieProperties = authCookieProperties;
		this.authRoutePolicy = authRoutePolicy;
		this.objectMapper = objectMapper;
	}

	/**
	 * 쿠키 보호 대상이 아닌 요청은 필터 적용에서 제외합니다.
	 *
	 * @param request 현재 HTTP 요청
	 * @return 필터 생략 여부
	 */
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !authRoutePolicy.isCookieGuardedPath(request);
	}

	/**
	 * 보호 대상 요청의 Origin 또는 Referer를 검증하고 허용되지 않으면 403 응답을 작성합니다.
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
		if (!hasAuthCookie(request) || hasAllowedOrigin(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), GlobalResponse.fail(ErrorCode.FORBIDDEN));
	}

	private boolean hasAuthCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return false;
		}
		for (Cookie cookie : cookies) {
			if (cookie == null) {
				continue;
			}
			String name = cookie.getName();
			if (authCookieProperties.refreshCookie().name().equals(name)
				|| ssoProperties.getSession().getCookieName().equals(name)
				|| authCookieProperties.accessCookie().name().equals(name)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasAllowedOrigin(HttpServletRequest request) {
		Optional<String> candidate = originHeader(request).or(() -> refererOrigin(request));
		if (candidate.isEmpty()) {
			return false;
		}
		String origin = normalizeOrigin(candidate.get());
		return ssoProperties.getFrontend().getAllowedOrigins().stream()
			.map(this::normalizeOrigin)
			.anyMatch(origin::equals);
	}

	private Optional<String> originHeader(HttpServletRequest request) {
		String origin = request.getHeader(HttpHeaders.ORIGIN);
		return origin == null || origin.isBlank() ? Optional.empty() : Optional.of(origin);
	}

	private Optional<String> refererOrigin(HttpServletRequest request) {
		String referer = request.getHeader(HttpHeaders.REFERER);
		if (referer == null || referer.isBlank()) {
			return Optional.empty();
		}
		try {
			URI uri = URI.create(referer);
			if (uri.getScheme() == null || uri.getHost() == null) {
				return Optional.empty();
			}
			String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
			return Optional.of(uri.getScheme() + "://" + uri.getHost() + port);
		} catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

	private String normalizeOrigin(String origin) {
		String trimmed = origin == null ? "" : origin.trim();
		if (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed.toLowerCase(Locale.ROOT);
	}
}
