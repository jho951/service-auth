package com.authservice.app.common.base.constant;

public enum ErrorCode {
	INVALID_REQUEST(400,  9015, "잘못된 요청입니다."),
	VALIDATION_ERROR(400, 9016, "요청 필드 유효성 검사에 실패했습니다."),
	METHOD_NOT_ALLOWED(405,  9017, "허용되지 않은 HTTP 메서드입니다."),
	NOT_FOUND_URL(404,9002, "요청하신 URL을 찾을 수 없습니다."),
	UNAUTHORIZED(401,  9101, "인증이 필요합니다."),
	FORBIDDEN(403,  9102, "접근이 허용되지 않습니다."),
	TOO_MANY_REQUESTS(429, 9103, "요청이 너무 많습니다."),
	PAYLOAD_TOO_LARGE(413,  9104, "요청 본문이 허용 크기를 초과했습니다."),
	UPSTREAM_TIMEOUT(504,9105, "업스트림 응답 시간이 초과되었습니다."),
	UPSTREAM_FAILURE(502,  9106, "업스트림 호출에 실패했습니다."),

	INVALID_TOKEN(401, 9111, "유효하지 않은 인증 토큰입니다."),
	EXPIRED_TOKEN(401,9112, "인증 토큰이 만료되었습니다."),
	NEED_LOGIN(401, 9113, "로그인이 필요한 요청입니다."),
	INVALID_REQUEST_DATA(400,  9003, "데이터 저장 실패, 재시도 혹은 관리자에게 문의해주세요."),
	CONFLICT_AUTH_ACCOUNT(409,  9005, "이미 존재하는 인증 계정입니다."),
	NOT_FOUND_AUTH_ACCOUNT(404,  9006, "인증 계정을 찾을 수 없습니다."),
	USER_SERVICE_UNAVAILABLE(502,  9007, "user-service 연동에 실패했습니다."),
	FAIL(400,9999, "요청 응답 실패, 관리자에게 문의해주세요.");

	private final int httpStatus;
	private final int code;
	private final String message;

	/**
	 * 생성자
	 * @param httpStatus 상태
	 * @param code 상태 코드
	 * @param message 추가 메시지
	 */
	ErrorCode(int httpStatus, int code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}

	public int getHttpStatus() {
		return httpStatus;
	}
	public int getCode() {
		return code;
	}
	public String getMessage() {
		return message;
	}
}
