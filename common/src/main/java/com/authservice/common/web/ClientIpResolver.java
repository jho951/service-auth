package com.authservice.common.web;

import com.authservice.common.logging.LoggingHeaders;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientIpResolver {

	public static String resolve(HttpServletRequest request) {
		if (request == null) return "unknown";

		String xff = request.getHeader(LoggingHeaders.X_FORWARDED_FOR);
		if (xff != null && !xff.isBlank()) {
			int comma = xff.indexOf(',');
			return comma > -1 ? xff.substring(0, comma).trim() : xff.trim();
		}

		String remoteAddr = request.getRemoteAddr();
		return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr;
	}
}
