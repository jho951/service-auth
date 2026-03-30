package com.authservice.app.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HttpAccessLogFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(HttpAccessLogFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		long startNanos = System.nanoTime();
		try {
			filterChain.doFilter(request, response);
		} finally {
			long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
			log.info("HTTP {} {} status={} elapsedMs={} ip={} ua={}",
				request.getMethod(),
				request.getRequestURI(),
				response.getStatus(),
				elapsedMs,
				resolveClientIp(request),
				request.getHeader("User-Agent")
			);
		}
	}

	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return true;
	}

	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return true;
	}

	private String resolveClientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (xff != null && !xff.isBlank()) {
			int comma = xff.indexOf(',');
			return comma > -1 ? xff.substring(0, comma).trim() : xff.trim();
		}
		String remoteAddr = request.getRemoteAddr();
		return remoteAddr == null ? "unknown" : remoteAddr;
	}
}
