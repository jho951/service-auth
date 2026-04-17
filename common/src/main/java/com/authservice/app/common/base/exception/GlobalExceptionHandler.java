package com.authservice.app.common.base.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.authservice.app.common.base.constant.ErrorCode;
import com.authservice.app.common.base.dto.GlobalResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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
			resolveClientIp(request),
			exceptionType,
			exceptionMessage);
	}

	private String resolveClientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (xff != null && !xff.isBlank()) {
			int comma = xff.indexOf(',');
			return comma > -1 ? xff.substring(0, comma).trim() : xff.trim();
		}
		String remoteAddr = request.getRemoteAddr();
		return remoteAddr == null ? "unknown" : remoteAddr;
	}
}
