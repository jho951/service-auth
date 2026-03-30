package com.authservice.app.common.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 애플리케이션에서 발생하는 모든 예외 및 에러를 처리하는 전역 에러 컨트롤러입니다.
 * <p>
 * Spring Boot의 기본 에러 핸들링 메커니즘을 오버라이드하거나 보조하여,
 * 클라이언트에게 표준화된 JSON 구조의 에러 응답을 반환합니다.
 * </p>
 */
@RestController
public class ErrorEndpointController {

	/**
	 * "/error" 경로로 유입되는 에러 요청을 처리하여 JSON 형식의 응답을 생성합니다.
	 * <p>
	 * {@link HttpServletRequest}의 속성에서 에러 상태 코드와 메시지를 추출하며,
	 * 정보가 누락된 경우 기본값으로 500(Internal Server Error)을 사용합니다.
	 * </p>
	 *
	 * @param request 서블릿 컨테이너로부터 전달받은 에러 정보를 포함한 HTTP 요청 객체
	 * @return 에러 상태 코드, 에러 유형, 상세 메시지를 포함한 {@link ResponseEntity} (JSON 형식)
	 */
	@RequestMapping(value = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {

		Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

		int rawStatus = statusCode instanceof Integer code ? code : 500;

		HttpStatus status = HttpStatus.resolve(rawStatus);
		if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

		String errorMessage = message instanceof String text && !text.isBlank()
			? text
			: status.getReasonPhrase();

		return ResponseEntity.status(status)
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of(
				"status", status.value(),
				"error", status.getReasonPhrase(),
				"message", errorMessage
			));
	}
}