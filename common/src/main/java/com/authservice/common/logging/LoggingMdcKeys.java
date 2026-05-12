package com.authservice.common.logging;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** MDC (Application Memory 영역)에 로그를 남길 때 공통 키(Key) */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LoggingMdcKeys {

	/** 개별 요청을 식별하기 위한 고유 ID 키입니다. (예: UUID) */
	public static final String REQUEST_ID = "request_id";
	/** 서비스 간의 연관된 요청을 묶어서 추적하기 위한 상관관계 ID 키입니다. */
	public static final String CORRELATION_ID = "correlation_id";
	/** 전체 실행 경로를 추적하기 위한 트레이스 ID 키입니다. */
	public static final String TRACE_ID = "trace_id";
	/** 호출된 HTTP 메서드 정보를 저장하기 위한 키입니다. (예: GET, POST, PUT, DELETE) */
	public static final String HTTP_METHOD = "http_method";
	/** 요청이 들어온 HTTP URI 정보를 저장하기 위한 키입니다. (예: /api/v1/auth/login) */
	public static final String REQUEST_URI = "request_uri";
	/** 요청을 보낸 클라이언트의 IP 주소를 저장하기 위한 키입니다. */
	public static final String CLIENT_IP = "client_ip";
	/** 현재 로그가 발생한 서비스의 이름을 저장하기 위한 키입니다. */
	public static final String SERVICE_NAME = "service_name";
}
