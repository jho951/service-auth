package com.authservice.common.base.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.authservice.common.base.constant.ErrorCode;
import com.authservice.common.base.dto.GlobalResponse;
import com.authservice.common.web.ClientIpResolver;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(GlobalException.class)
	public ResponseEntity<GlobalResponse<Void>> handleGlobalException(GlobalException ex, HttpServletRequest request) {
		ErrorCode errorCode = ex.getErrorCode();
		logWithRequest(request, errorCode, ex.getClass().getSimpleName(), ex.getMessage());
		return ResponseEntity.status(errorCode.getHttpStatus())
			.body(GlobalResponse.fail(errorCode));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<GlobalResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
		logWithRequest(request, ErrorCode.INVALID_REQUEST, ex.getClass().getSimpleName(), ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(GlobalResponse.fail(ErrorCode.INVALID_REQUEST));
	}

	private void logWithRequest(HttpServletRequest request, ErrorCode errorCode, String exceptionType, String exceptionMessage) {
		if (request == null) {
			log.warn("Request failed with status={} code={} exceptionType={} message={}",
				errorCode.getHttpStatus(),
				errorCode.getCode(),
				exceptionType,
				exceptionMessage);
			return;
		}

		log.warn("Request failed with status={} code={} method={} uri={} ip={} exceptionType={} message={}",
			errorCode.getHttpStatus(),
			errorCode.getCode(),
			request.getMethod(),
			request.getRequestURI(),
			ClientIpResolver.resolve(request),
			exceptionType,
			exceptionMessage);
	}
}
