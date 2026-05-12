package com.authservice.common.base.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SuccessCode {
	/** 조회 성공 (GET) */
	GET_SUCCESS(200, 1, "조회 요청 성공"),
	/** 생성 성공 (POST) */
	CREATE_SUCCESS(201, 2, "리소스 생성 성공"),
	/**  수정 성공 (PUT, PATCH) */
	UPDATE_SUCCESS(200, 3, "리소스 수정 성공"),
	/**  삭제 성공 (DELETE) */
	DELETE_SUCCESS(200, 4, "리소스 삭제 성공"),
	/**  비동기 요청 접수 */
	PROCESS_ACCEPTED(202, 5, "요청 접수 성공");

	/** http 상태 */
	private final int httpStatus;
	/** 유지보수를 위한 상태 코드 */
	private final int code;
	/** 추가 메시지 */
	private final String message;
}
