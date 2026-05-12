package com.authservice.app.domain.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieProperties {

	private final CookieSettings refreshCookie;
	private final CookieSettings accessCookie;

	public AuthCookieProperties(
		AuthHttpProperties authProperties,
		@Value("${AUTH_ACCESS_COOKIE_NAME:ACCESS_TOKEN}") String accessCookieName,
		@Value("${AUTH_ACCESS_COOKIE_HTTP_ONLY:true}") boolean accessCookieHttpOnly,
		@Value("${AUTH_ACCESS_COOKIE_SECURE:${SSO_SESSION_COOKIE_SECURE:false}}") boolean accessCookieSecure,
		@Value("${AUTH_ACCESS_COOKIE_SAME_SITE:${SSO_SESSION_COOKIE_SAME_SITE:Lax}}") String accessCookieSameSite,
		@Value("${AUTH_ACCESS_COOKIE_PATH:/}") String accessCookiePath
	) {
		this.refreshCookie = new CookieSettings(
			authProperties.getRefreshCookieName(),
			authProperties.isRefreshCookieHttpOnly(),
			authProperties.isRefreshCookieSecure(),
			authProperties.getRefreshCookiePath(),
			authProperties.getRefreshCookieSameSite(),
			authProperties.getJwt().getRefreshSeconds(),
			authProperties.isRefreshCookieEnabled()
		);
		this.accessCookie = new CookieSettings(
			accessCookieName,
			accessCookieHttpOnly,
			accessCookieSecure,
			accessCookiePath,
			accessCookieSameSite,
			authProperties.getJwt().getAccessSeconds(),
			true
		);
	}

	public CookieSettings refreshCookie() {
		return refreshCookie;
	}

	public CookieSettings accessCookie() {
		return accessCookie;
	}

	public record CookieSettings(
		String name,
		boolean httpOnly,
		boolean secure,
		String path,
		String sameSite,
		long ttlSeconds,
		boolean enabled
	) {
	}
}
