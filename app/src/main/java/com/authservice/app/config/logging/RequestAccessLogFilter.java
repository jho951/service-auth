package com.authservice.app.config.logging;

import com.authservice.common.logging.LoggingMdcKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestAccessLogFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestAccessLogFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		long startNanos = System.nanoTime();
		try {
			filterChain.doFilter(request, response);
		} finally {
			long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
			log.info("event=http_request_completed status={} elapsed_ms={} client_ip={} user_agent={}",
				response.getStatus(),
				elapsedMs,
				MDC.get(LoggingMdcKeys.CLIENT_IP),
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
}
