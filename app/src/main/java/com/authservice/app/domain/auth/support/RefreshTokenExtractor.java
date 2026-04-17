package com.authservice.app.domain.auth.support;

import com.authservice.app.common.base.constant.ErrorCode;
import com.authservice.app.common.base.exception.GlobalException;
import com.authservice.app.domain.auth.config.AuthHttpProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

public class RefreshTokenExtractor {

	private final AuthHttpProperties properties;

	public RefreshTokenExtractor(AuthHttpProperties properties) {
		this.properties = properties;
	}

	public String extract(HttpServletRequest request) {
		return extractOptional(request)
			.orElseThrow(() -> new GlobalException(ErrorCode.INVALID_REQUEST));
	}

	public Optional<String> extractOptional(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie != null && properties.getRefreshCookieName().equals(cookie.getName())) {
					String value = cookie.getValue();
					if (value != null && !value.isBlank()) {
						return Optional.of(value);
					}
				}
			}
		}
		return Optional.empty();
	}
}
