package com.authservice.app.domain.auth.support;

import com.authservice.app.domain.auth.config.AuthCookieProperties;
import com.authservice.app.domain.auth.config.AuthCookieProperties.CookieSettings;
import com.authservice.app.domain.auth.model.AuthTokens;
import java.util.ArrayList;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieResponseWriter {

	private final AuthCookieProperties cookieProperties;

	public AuthCookieResponseWriter(AuthCookieProperties cookieProperties) {
		this.cookieProperties = cookieProperties;
	}

	public String accessTokenCookieName() {
		return cookieProperties.accessCookie().name();
	}

	public String buildAccessTokenCookie(String accessToken) {
		return buildCookie(cookieProperties.accessCookie(), accessToken, cookieProperties.accessCookie().ttlSeconds());
	}

	public String clearAccessTokenCookie() {
		return buildCookie(cookieProperties.accessCookie(), "", 0);
	}

	public <T> ResponseEntity<T> writeTokenCookies(AuthTokens tokens, ResponseEntity<T> response) {
		ResponseEntity<T> withAccessCookie = appendCookies(response, buildAccessTokenCookie(tokens.accessToken()));
		if (!cookieProperties.refreshCookie().enabled()) {
			return withAccessCookie;
		}
		return appendCookies(
			withAccessCookie,
			buildCookie(cookieProperties.refreshCookie(), tokens.refreshToken(), cookieProperties.refreshCookie().ttlSeconds())
		);
	}

	public ResponseEntity<Void> clearTokenCookies(ResponseEntity<Void> response) {
		ResponseEntity<Void> cleared = appendCookies(response, clearAccessTokenCookie());
		if (!cookieProperties.refreshCookie().enabled()) {
			return cleared;
		}
		return appendCookies(cleared, buildCookie(cookieProperties.refreshCookie(), "", 0));
	}

	public <T> ResponseEntity<T> appendCookies(ResponseEntity<T> response, String... cookies) {
		HttpHeaders headers = new HttpHeaders();
		response.getHeaders().forEach((name, values) -> headers.put(name, new ArrayList<>(values)));
		for (String cookie : cookies) {
			if (cookie != null && !cookie.isBlank()) {
				headers.add(HttpHeaders.SET_COOKIE, cookie);
			}
		}
		return ResponseEntity.status(response.getStatusCode())
			.headers(headers)
			.body(response.getBody());
	}

	private String buildCookie(CookieSettings settings, String value, long maxAge) {
		return ResponseCookie.from(settings.name(), value)
			.httpOnly(settings.httpOnly())
			.secure(settings.secure())
			.path(settings.path())
			.sameSite(settings.sameSite())
			.maxAge(maxAge)
			.build()
			.toString();
	}
}
