package com.authservice.app.common.base.exception;

import com.auth.api.exception.AuthException;
import com.auth.api.exception.AuthFailureReason;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.authservice.app.common.base.constant.ErrorCode;
import com.authservice.app.common.base.dto.GlobalResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(GlobalException.class)
	public ResponseEntity<GlobalResponse<Void>> handleGlobalException(GlobalException ex) {
		ErrorCode errorCode = ex.getErrorCode();
		return ResponseEntity.status(errorCode.getHttpStatus())
			.body(GlobalResponse.fail(errorCode));
	}

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<GlobalResponse<Void>> handleAuthException(AuthException ex) {
		ErrorCode errorCode = mapAuthError(ex);
		return ResponseEntity.status(errorCode.getHttpStatus())
			.body(GlobalResponse.fail(errorCode));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<GlobalResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(GlobalResponse.fail(ErrorCode.INVALID_REQUEST));
	}

	private ErrorCode mapAuthError(AuthException ex) {
		return switch (ex.getReason()) {
			case INVALID_CREDENTIALS, USER_NOT_FOUND -> ErrorCode.UNAUTHORIZED;
			case INVALID_TOKEN, REVOKED_TOKEN -> ErrorCode.INVALID_TOKEN;
			case INVALID_INPUT -> ErrorCode.INVALID_REQUEST;
			default -> ErrorCode.FAIL;
		};
	}
}
