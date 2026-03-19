package com.authservice.app.domain.auth.service;

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
		String xff = request.getHeader("X-Forwarded-For");
		String ip = (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
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
