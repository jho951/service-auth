package com.authservice.app.security;

import com.authservice.common.base.constant.ErrorCode;
import com.authservice.common.base.dto.GlobalResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthHandlers {

	private static final Logger log = LoggerFactory.getLogger(RestAuthHandlers.class);

	@Component
	public static class EntryPoint implements AuthenticationEntryPoint {
		@Override
		public void commence(HttpServletRequest req, HttpServletResponse res,
			org.springframework.security.core.AuthenticationException e) throws IOException {
			log.warn("Unauthorized request rejected by security. method={} uri={} ip={} exceptionType={} message={}",
				req.getMethod(),
				req.getRequestURI(),
				resolveClientIp(req),
				e.getClass().getSimpleName(),
				e.getMessage());
			res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			res.setContentType(MediaType.APPLICATION_JSON_VALUE);
			new ObjectMapper().writeValue(res.getOutputStream(), GlobalResponse.fail(ErrorCode.UNAUTHORIZED));
		}
	}

	@Component
	public static class Denied implements AccessDeniedHandler {
		@Override
		public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException e) throws IOException {
			res.setStatus(HttpServletResponse.SC_FORBIDDEN);
			res.setContentType(MediaType.APPLICATION_JSON_VALUE);
			new ObjectMapper().writeValue(res.getOutputStream(), GlobalResponse.fail(ErrorCode.FORBIDDEN));
		}
	}

	private static String resolveClientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (xff != null && !xff.isBlank()) {
			int comma = xff.indexOf(',');
			return comma > -1 ? xff.substring(0, comma).trim() : xff.trim();
		}
		String remoteAddr = request.getRemoteAddr();
		return remoteAddr == null ? "unknown" : remoteAddr;
	}
}
