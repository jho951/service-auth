package com.authservice.app.common.logging;

public final class LoggingMdcKeys {

	public static final String REQUEST_ID = "request_id";
	public static final String CORRELATION_ID = "correlation_id";
	public static final String TRACE_ID = "trace_id";
	public static final String HTTP_METHOD = "http_method";
	public static final String REQUEST_URI = "request_uri";
	public static final String CLIENT_IP = "client_ip";
	public static final String SERVICE_NAME = "service_name";

	private LoggingMdcKeys() {
	}
}
