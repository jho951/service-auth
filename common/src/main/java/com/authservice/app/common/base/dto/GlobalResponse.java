package com.authservice.app.common.base.dto;

import lombok.Getter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.authservice.app.common.base.constant.ErrorCode;
import com.authservice.app.common.base.constant.SuccessCode;

@Getter
@Schema(description = "공통 응답 구조")
public final class GlobalResponse<T> {

	@Schema(description = "HTTP 상태 코드", example = "200")
	private final int httpStatus;
	@Schema(description = "성공 여부", example = "true")
	private final boolean success;
	@Schema(description = "응답 메시지", example = "요청이 성공적으로 처리되었습니다.")
	private final String message;
	@Schema(description = "비즈니스 코드", example = "1000")
	private final int code;
	@Schema(description = "응답 데이터")
	private final T data;

	public GlobalResponse(int httpStatus, boolean success, String message, int code, T data) {
		if (message == null) throw new IllegalArgumentException("메시지는 null일 수 없습니다.");
		this.httpStatus = httpStatus;
		this.success = success;
		this.message = message;
		this.code = code;
		this.data = data;
	}

	public static <T> GlobalResponse<T> ok(SuccessCode successCode, T data) {
		if (successCode == null || data == null) throw new IllegalArgumentException("HTTP 상태와 메시지는 null일 수 없습니다.");
		return new GlobalResponse<>(
			successCode.getHttpStatus(),
			true,
			successCode.getMessage(),
			successCode.getCode(),
			data
		);
	}

	public static GlobalResponse<Void> ok(SuccessCode successCode) {
		if (successCode == null) throw new IllegalArgumentException("HTTP 상태와 메시지는 null일 수 없습니다.");
		return new GlobalResponse<>(
			successCode.getHttpStatus(),
			true,
			successCode.getMessage(),
			successCode.getCode(),
			null
		);
	}

	public static GlobalResponse<Void> fail(ErrorCode errorCode) {
		if (errorCode == null) throw new IllegalArgumentException("HTTP 상태와 메시지는 null일 수 없습니다.");
		return new GlobalResponse<>(
			errorCode.getHttpStatus(),
			false,
			errorCode.getMessage(),
			errorCode.getCode(),
			null
		);
	}
}
