package com.authservice.common.base.exception;

import com.authservice.common.base.constant.ErrorCode;

public class GlobalException extends RuntimeException {
	private final ErrorCode errorCode;

	public GlobalException(final ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
	public ErrorCode getErrorCode() {
		return errorCode;
	}
}