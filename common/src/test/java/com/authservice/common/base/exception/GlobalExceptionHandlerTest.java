package com.authservice.common.base.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.authservice.common.base.constant.ErrorCode;
import com.authservice.common.base.dto.GlobalResponse;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void handlesGlobalExceptionUsingContainedErrorCode() {
		GlobalException exception = new GlobalException(ErrorCode.NOT_FOUND_AUTH_ACCOUNT);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/1");
		request.setRemoteAddr("127.0.0.1");

		ResponseEntity<GlobalResponse<Void>> response = handler.handleGlobalException(exception, request);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertNotNull(response.getBody());
		assertFalse(response.getBody().isSuccess());
		assertEquals(ErrorCode.NOT_FOUND_AUTH_ACCOUNT.getCode(), response.getBody().getCode());
		assertEquals(ErrorCode.NOT_FOUND_AUTH_ACCOUNT.getMessage(), response.getBody().getMessage());
	}

	@Test
	void handlesIllegalArgumentExceptionAsInvalidRequest() {
		IllegalArgumentException exception = new IllegalArgumentException("bad input");

		ResponseEntity<GlobalResponse<Void>> response = handler.handleIllegalArgumentException(exception, null);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		assertFalse(response.getBody().isSuccess());
		assertEquals(ErrorCode.INVALID_REQUEST.getCode(), response.getBody().getCode());
		assertEquals(ErrorCode.INVALID_REQUEST.getMessage(), response.getBody().getMessage());
	}
}
