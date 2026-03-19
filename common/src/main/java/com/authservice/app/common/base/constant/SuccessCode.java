package com.authservice.app.common.base.constant;

public enum SuccessCode {
	SUCCESS(200, true, 200, "요청 응답 성공");

	private final int httpStatus;
	private final boolean success;
	private final int code;
	private final String message;

	SuccessCode(int httpStatus, boolean success, int code, String message) {
		this.httpStatus = httpStatus;
		this.success = success;
		this.code = code;
		this.message = message;
	}

	public int getHttpStatus() {
		return httpStatus;
	}

	public boolean isSuccess() {
		return success;
	}

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
