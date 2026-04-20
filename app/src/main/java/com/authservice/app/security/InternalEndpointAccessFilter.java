package com.authservice.app.security;

import com.authservice.common.base.constant.ErrorCode;
import com.authservice.common.base.dto.GlobalResponse;
import com.authservice.common.base.exception.GlobalException;
import com.authservice.app.domain.auth.userdirectory.config.InternalApiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalEndpointAccessFilter extends OncePerRequestFilter {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final InternalApiProperties internalApiProperties;

	public InternalEndpointAccessFilter(InternalApiProperties internalApiProperties) {
		this.internalApiProperties = internalApiProperties;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		String contextPath = request.getContextPath();
		if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
			path = path.substring(contextPath.length());
		}
		return !isInternalPath(path);
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		try {
			internalApiProperties.validateInternalAccess(
				request.getHeader(HttpHeaders.AUTHORIZATION),
				request.getHeader(InternalApiProperties.INTERNAL_SECRET_HEADER)
			);
		} catch (GlobalException ex) {
			writeUnauthorized(response);
			return;
		}

		filterChain.doFilter(request, response);
	}

	private static boolean isInternalPath(String path) {
		return path.equals("/internal")
			|| path.startsWith("/internal/")
			|| path.equals("/auth/internal")
			|| path.startsWith("/auth/internal/");
	}

	private static void writeUnauthorized(HttpServletResponse response) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		OBJECT_MAPPER.writeValue(response.getOutputStream(), GlobalResponse.fail(ErrorCode.UNAUTHORIZED));
	}
}
