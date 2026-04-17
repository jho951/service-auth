package com.authservice.app.domain.auth.sso.service;

import com.authservice.app.domain.auth.config.AuthHttpProperties;
import com.authservice.app.domain.auth.sso.config.SsoProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class SsoCookieService {

	private static final String OAUTH_STATE_COOKIE_NAME = "sso_oauth_state";

	private final SsoProperties properties;
	private final String accessTokenCookieName;
	private final long accessTokenTtlSeconds;
	private final boolean accessTokenCookieHttpOnly;
	private final boolean accessTokenCookieSecure;
	private final String accessTokenCookieSameSite;
	private final String accessTokenCookiePath;

	public SsoCookieService(
		SsoProperties properties,
		AuthHttpProperties authProperties,
		@Value("${AUTH_ACCESS_COOKIE_NAME:ACCESS_TOKEN}") String accessTokenCookieName,
		@Value("${AUTH_ACCESS_COOKIE_HTTP_ONLY:true}") boolean accessTokenCookieHttpOnly,
		@Value("${AUTH_ACCESS_COOKIE_SECURE:${SSO_SESSION_COOKIE_SECURE:false}}") boolean accessTokenCookieSecure,
		@Value("${AUTH_ACCESS_COOKIE_SAME_SITE:${SSO_SESSION_COOKIE_SAME_SITE:Lax}}") String accessTokenCookieSameSite,
		@Value("${AUTH_ACCESS_COOKIE_PATH:/}") String accessTokenCookiePath
	) {
		this.properties = properties;
		this.accessTokenCookieName = accessTokenCookieName;
		this.accessTokenTtlSeconds = authProperties.getJwt().getAccessSeconds();
		this.accessTokenCookieHttpOnly = accessTokenCookieHttpOnly;
		this.accessTokenCookieSecure = accessTokenCookieSecure;
		this.accessTokenCookieSameSite = accessTokenCookieSameSite;
		this.accessTokenCookiePath = accessTokenCookiePath;
	}

	public Optional<String> extractSessionId(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		return Arrays.stream(cookies)
			.filter(cookie -> properties.getSession().getCookieName().equals(cookie.getName()))
			.map(Cookie::getValue)
			.findFirst();
	}

	public ResponseEntity<Void> writeSessionCookie(String sessionId) {
		String sessionCookie = buildSessionCookie(sessionId);
		return ResponseEntity.noContent()
			.header(HttpHeaders.SET_COOKIE, sessionCookie)
			.build();
	}

	public String buildSessionCookie(String sessionId) {
		ResponseCookie cookie = ResponseCookie.from(properties.getSession().getCookieName(), sessionId)
			.httpOnly(properties.getSession().isCookieHttpOnly())
			.secure(properties.getSession().isCookieSecure())
			.path(properties.getSession().getCookiePath())
			.sameSite(properties.getSession().getCookieSameSite())
			.maxAge(properties.getSession().getTtlSeconds())
			.build();
		return cookie.toString();
	}

	public Optional<String> extractOAuthState(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		return Arrays.stream(cookies)
			.filter(cookie -> OAUTH_STATE_COOKIE_NAME.equals(cookie.getName()))
			.map(Cookie::getValue)
			.findFirst();
	}

	public String buildOAuthStateCookie(String state, long maxAgeSeconds) {
		return ResponseCookie.from(OAUTH_STATE_COOKIE_NAME, state)
			.httpOnly(true)
			.secure(properties.getSession().isCookieSecure())
			.path("/")
			.sameSite(properties.getSession().getCookieSameSite())
			.maxAge(maxAgeSeconds)
			.build()
			.toString();
	}

	public String clearOAuthStateCookie() {
		return ResponseCookie.from(OAUTH_STATE_COOKIE_NAME, "")
			.httpOnly(true)
			.secure(properties.getSession().isCookieSecure())
			.path("/")
			.sameSite(properties.getSession().getCookieSameSite())
			.maxAge(0)
			.build()
			.toString();
	}

	public ResponseEntity<Void> clearSessionCookie() {
		String sessionCookie = clearSessionCookieValue();
		return ResponseEntity.noContent()
			.header(HttpHeaders.SET_COOKIE, sessionCookie)
			.build();
	}

	public String clearSessionCookieValue() {
		ResponseCookie cookie = ResponseCookie.from(properties.getSession().getCookieName(), "")
			.httpOnly(properties.getSession().isCookieHttpOnly())
			.secure(properties.getSession().isCookieSecure())
			.path(properties.getSession().getCookiePath())
			.sameSite(properties.getSession().getCookieSameSite())
			.maxAge(0)
			.build();
		return cookie.toString();
	}

	public String buildAccessTokenCookie(String accessToken) {
		return ResponseCookie.from(accessTokenCookieName, accessToken)
			.httpOnly(accessTokenCookieHttpOnly)
			.secure(accessTokenCookieSecure)
			.path(accessTokenCookiePath)
			.sameSite(accessTokenCookieSameSite)
			.maxAge(accessTokenTtlSeconds)
			.build()
			.toString();
	}

	public String clearAccessTokenCookie() {
		return ResponseCookie.from(accessTokenCookieName, "")
			.httpOnly(accessTokenCookieHttpOnly)
			.secure(accessTokenCookieSecure)
			.path(accessTokenCookiePath)
			.sameSite(accessTokenCookieSameSite)
			.maxAge(0)
			.build()
			.toString();
	}
}
