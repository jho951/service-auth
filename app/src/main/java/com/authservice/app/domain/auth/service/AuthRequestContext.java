package com.authservice.app.domain.auth.service;

import com.authservice.common.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;

public final class AuthRequestContext {

	private final String ip;
	private final String userAgent;

	private AuthRequestContext(String ip, String userAgent) {
		this.ip = ip;
		this.userAgent = userAgent;
	}

	public static AuthRequestContext from(HttpServletRequest request) {
		if (request == null) {
			return new AuthRequestContext("", "");
		}
		String ip = ClientIpResolver.resolve(request);
		String userAgent = request.getHeader("User-Agent");
		return new AuthRequestContext(ip == null ? "" : ip, userAgent == null ? "" : userAgent);
	}

	public String ip() {
		return ip;
	}

	public String userAgent() {
		return userAgent;
	}
}
