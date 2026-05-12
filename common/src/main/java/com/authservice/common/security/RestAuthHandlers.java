package com.authservice.common.security;

import com.authservice.common.base.constant.ErrorCode;
import com.authservice.common.base.dto.GlobalResponse;
import com.authservice.common.web.ClientIpResolver;
import java.io.IOException;
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

public class RestAuthHandlers {

	private static final Logger log = LoggerFactory.getLogger(RestAuthHandlers.class);

	@Component
	public static class EntryPoint implements AuthenticationEntryPoint {

		private final ObjectMapper objectMapper;

		public EntryPoint(ObjectMapper objectMapper) {
			this.objectMapper = objectMapper;
		}

		@Override
		public void commence(HttpServletRequest req, HttpServletResponse res,
			org.springframework.security.core.AuthenticationException e) throws IOException {
			log.warn("Unauthorized request rejected by security. method={} uri={} ip={} exceptionType={} message={}",
				req.getMethod(),
				req.getRequestURI(),
				ClientIpResolver.resolve(req),
				e.getClass().getSimpleName(),
				e.getMessage());
			res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			res.setContentType(MediaType.APPLICATION_JSON_VALUE);
			objectMapper.writeValue(res.getOutputStream(), GlobalResponse.fail(ErrorCode.UNAUTHORIZED));
		}
	}

	@Component
	public static class Denied implements AccessDeniedHandler {

		private final ObjectMapper objectMapper;

		public Denied(ObjectMapper objectMapper) {
			this.objectMapper = objectMapper;
		}

		@Override
		public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException e) throws IOException {
			res.setStatus(HttpServletResponse.SC_FORBIDDEN);
			res.setContentType(MediaType.APPLICATION_JSON_VALUE);
			objectMapper.writeValue(res.getOutputStream(), GlobalResponse.fail(ErrorCode.FORBIDDEN));
		}
	}
}
