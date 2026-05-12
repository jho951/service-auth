package com.authservice.common.base.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.authservice.common.base.constant.ErrorCode;
import com.authservice.common.base.constant.SuccessCode;

class GlobalResponseTest {

	@Test
	void constructorRejectsNullMessage() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> new GlobalResponse<>(200, true, null, 1, "data"));

		assertEquals("응답 메시지는 null일 수 없습니다.", exception.getMessage());
	}

	@Test
	void okWithDataBuildsSuccessResponse() {
		GlobalResponse<String> response = GlobalResponse.ok(SuccessCode.GET_SUCCESS, "payload");

		assertEquals(SuccessCode.GET_SUCCESS.getHttpStatus(), response.getHttpStatus());
		assertTrue(response.isSuccess());
		assertEquals(SuccessCode.GET_SUCCESS.getMessage(), response.getMessage());
		assertEquals(SuccessCode.GET_SUCCESS.getCode(), response.getCode());
		assertEquals("payload", response.getData());
	}

	@Test
	void okWithDataRejectsNullArguments() {
		IllegalArgumentException nullCode = assertThrows(IllegalArgumentException.class,
			() -> GlobalResponse.ok(null, "payload"));
		IllegalArgumentException nullData = assertThrows(IllegalArgumentException.class,
			() -> GlobalResponse.ok(SuccessCode.GET_SUCCESS, null));

		assertEquals("성공 코드는 null일 수 없습니다.", nullCode.getMessage());
		assertEquals("응답 데이터는 null일 수 없습니다.", nullData.getMessage());
	}

	@Test
	void okWithoutDataBuildsSuccessResponse() {
		GlobalResponse<Void> response = GlobalResponse.ok(SuccessCode.CREATE_SUCCESS);

		assertEquals(SuccessCode.CREATE_SUCCESS.getHttpStatus(), response.getHttpStatus());
		assertTrue(response.isSuccess());
		assertEquals(SuccessCode.CREATE_SUCCESS.getMessage(), response.getMessage());
		assertEquals(SuccessCode.CREATE_SUCCESS.getCode(), response.getCode());
		assertNull(response.getData());
	}

	@Test
	void okWithoutDataRejectsNullCode() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> GlobalResponse.ok((SuccessCode) null));

		assertEquals("성공 여부는 null일 수 없습니다.", exception.getMessage());
	}

	@Test
	void failBuildsFailureResponse() {
		GlobalResponse<Void> response = GlobalResponse.fail(ErrorCode.INVALID_REQUEST);

		assertEquals(ErrorCode.INVALID_REQUEST.getHttpStatus(), response.getHttpStatus());
		assertFalse(response.isSuccess());
		assertEquals(ErrorCode.INVALID_REQUEST.getMessage(), response.getMessage());
		assertEquals(ErrorCode.INVALID_REQUEST.getCode(), response.getCode());
		assertNull(response.getData());
	}

	@Test
	void failRejectsNullCode() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> GlobalResponse.fail(null));

		assertEquals("실패 여부는 null일 수 없습니다.", exception.getMessage());
	}
}
