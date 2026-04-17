package com.authservice.app.domain.auth.support;

import com.authservice.app.domain.auth.config.AuthHttpProperties;
import com.authservice.app.domain.auth.model.AuthTokens;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

public class RefreshCookieWriter {

	private final AuthHttpProperties properties;

	public RefreshCookieWriter(AuthHttpProperties properties) {
		this.properties = properties;
	}

	public <T> ResponseEntity<T> write(AuthTokens tokens, ResponseEntity<T> response) {
		if (!properties.isRefreshCookieEnabled()) {
			return response;
		}
		ResponseCookie cookie = baseCookie(tokens.refreshToken())
			.maxAge(properties.getJwt().getRefreshSeconds())
			.build();
		return ResponseEntity.status(response.getStatusCode())
			.headers(response.getHeaders())
			.header(HttpHeaders.SET_COOKIE, cookie.toString())
			.body(response.getBody());
	}

	public ResponseEntity<Void> clear(ResponseEntity<Void> response) {
		if (!properties.isRefreshCookieEnabled()) {
			return response;
		}
		ResponseCookie cookie = baseCookie("")
			.maxAge(0)
			.build();
		return ResponseEntity.status(response.getStatusCode())
			.headers(response.getHeaders())
			.header(HttpHeaders.SET_COOKIE, cookie.toString())
			.build();
	}

	private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
		return ResponseCookie.from(properties.getRefreshCookieName(), value)
			.httpOnly(properties.isRefreshCookieHttpOnly())
			.secure(properties.isRefreshCookieSecure())
			.path(properties.getRefreshCookiePath())
			.sameSite(properties.getRefreshCookieSameSite());
	}
}
