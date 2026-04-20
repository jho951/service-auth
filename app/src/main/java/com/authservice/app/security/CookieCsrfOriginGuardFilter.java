package com.authservice.app.security;

import com.authservice.common.base.constant.ErrorCode;
import com.authservice.common.base.dto.GlobalResponse;
import com.authservice.app.domain.auth.config.AuthHttpProperties;
import com.authservice.app.domain.auth.sso.config.SsoProperties;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CookieCsrfOriginGuardFilter extends OncePerRequestFilter {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final SsoProperties ssoProperties;
	private final AuthHttpProperties authProperties;
	private final String accessTokenCookieName;

	public CookieCsrfOriginGuardFilter(
		SsoProperties ssoProperties,
		AuthHttpProperties authProperties,
		@Value("${AUTH_ACCESS_COOKIE_NAME:ACCESS_TOKEN}") String accessTokenCookieName
	) {
		this.ssoProperties = ssoProperties;
		this.authProperties = authProperties;
		this.accessTokenCookieName = accessTokenCookieName;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !"POST".equalsIgnoreCase(request.getMethod()) || !isGuardedPath(request);
	}

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
		OBJECT_MAPPER.writeValue(response.getOutputStream(), GlobalResponse.fail(ErrorCode.FORBIDDEN));
	}

	private boolean isGuardedPath(HttpServletRequest request) {
		String path = request.getRequestURI();
		String contextPath = request.getContextPath();
		if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
			path = path.substring(contextPath.length());
		}
		return path.equals("/auth/refresh") || path.equals("/auth/logout");
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
			if (authProperties.getRefreshCookieName().equals(name)
				|| ssoProperties.getSession().getCookieName().equals(name)
				|| accessTokenCookieName.equals(name)) {
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
