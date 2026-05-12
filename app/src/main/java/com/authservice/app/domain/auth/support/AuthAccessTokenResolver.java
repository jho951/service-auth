package com.authservice.app.domain.auth.support;

import com.authservice.app.domain.auth.config.AuthCookieProperties;
import com.authservice.app.domain.auth.config.AuthHttpProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class AuthAccessTokenResolver {

	private final AuthCookieProperties authCookieProperties;
	private final AuthHttpProperties authHttpProperties;

	public AuthAccessTokenResolver(
		AuthCookieProperties authCookieProperties,
		AuthHttpProperties authHttpProperties
	) {
		this.authCookieProperties = authCookieProperties;
		this.authHttpProperties = authHttpProperties;
	}

	public Optional<String> resolve(HttpServletRequest request) {
		return resolveFromCookie(request).or(() -> resolveFromAuthorizationHeader(request));
	}

	private Optional<String> resolveFromCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		return Arrays.stream(cookies)
			.filter(cookie -> authCookieProperties.accessCookie().name().equals(cookie.getName()))
			.map(Cookie::getValue)
			.filter(value -> value != null && !value.isBlank())
			.findFirst();
	}

	private Optional<String> resolveFromAuthorizationHeader(HttpServletRequest request) {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		String bearerPrefix = authHttpProperties.getBearerPrefix();
		if (authorization == null || authorization.isBlank() || bearerPrefix == null || bearerPrefix.isBlank()) {
			return Optional.empty();
		}
		if (!authorization.startsWith(bearerPrefix)) {
			return Optional.empty();
		}
		String token = authorization.substring(bearerPrefix.length()).trim();
		return token.isBlank() ? Optional.empty() : Optional.of(token);
	}
}
