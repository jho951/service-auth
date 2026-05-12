package com.authservice.common.logging;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** HTTP Header (Network 영역) 에 담기는 공통 키(Key) */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LoggingHeaders {

	/** 클라이언트와 서버 간의 단일 트랜잭션을 식별하는 데 사용됩니다. */
	public static final String REQUEST_ID = "X-Request-Id";
	/** 여러 서비스 간의 비즈니스 흐름을 연결하기 위해 사용됩니다. */
	public static final String CORRELATION_ID = "X-Correlation-Id";
	/** 분산 추적 시스템(Jaeger, Zipkin 등)에서 트레이스 정보를 전달하는 표준 방식입니다. */
	public static final String TRACEPARENT = "traceparent";
	/** 프록시 서버나 로드밸런서를 거쳐올 때 실제 접속자의 IP를 확인하기 위해 사용됩니다. */
	public static final String X_FORWARDED_FOR = "X-Forwarded-For";
}
