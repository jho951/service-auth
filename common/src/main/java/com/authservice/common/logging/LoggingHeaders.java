package com.authservice.common.logging;

public final class LoggingHeaders {

	public static final String REQUEST_ID = "X-Request-Id";
	public static final String CORRELATION_ID = "X-Correlation-Id";
	public static final String TRACEPARENT = "traceparent";
	public static final String X_FORWARDED_FOR = "X-Forwarded-For";

	private LoggingHeaders() {
	}
}
