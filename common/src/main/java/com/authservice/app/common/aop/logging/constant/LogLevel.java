package com.authservice.app.common.aop.logging.constant;

import com.authservice.app.common.aop.logging.annotation.Loggable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 로그 레벨을 정의하는 열거형입니다.
 * 각 레벨은 SLF4J 및 Logback 등에서 사용하는 로그 심각도 수준을 나타냅니다.
 * Enum은 {@link Loggable}에서
 * 로그 출력 수준을 설정할 때 사용됩니다.
 * 문자열 레벨 값도 함께 포함되어 있어
 * 로그 문자열 출력 시 포맷 조정 등에 활용할 수 있습니다.
 */
@Getter
@RequiredArgsConstructor
public enum LogLevel {
	/** TRACE 레벨 - 가장 상세한 로그, 디버깅용 */
	TRACE("trace"),

	/** DEBUG 레벨 - 개발/테스트 중 사용되는 상세 로그 */
	DEBUG("debug"),

	/** INFO 레벨 - 운영환경에서의 정상 흐름 정보 로그 */
	INFO("info"),

	/** WARN 레벨 - 경고 상황 발생, 예외는 아님 */
	WARN("warn"),

	/** ERROR 레벨 - 예외 발생, 시스템에 문제가 있는 경우 */
	ERROR("error");

	/** 로그 레벨에 해당하는 문자열 표현 (예: "info", "debug") */
	private final String level;
}
