package com.authservice.app.common.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 애플리케이션에서 발생하는 모든 예외 및 에러를 처리하는 전역 에러 컨트롤러입니다. */
@RestController
public class ErrorEndpointController implements ErrorController {

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
		HttpStatus status = resolveStatus(request);
		String errorMessage = resolveMessage(request, status);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", status.value());
		body.put("error", status.getReasonPhrase());
		body.put("message", errorMessage);

		putIfPresent(body, "path", stringAttribute(request, RequestDispatcher.ERROR_REQUEST_URI));
		putIfPresent(body, "requestId", header(request, "X-Request-Id"));
		putIfPresent(body, "correlationId", header(request, "X-Correlation-Id"));

		return ResponseEntity.status(status)
			.contentType(MediaType.APPLICATION_JSON)
			.body(body);
	}

	private HttpStatus resolveStatus(HttpServletRequest request) {
		Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		int rawStatus = statusCode instanceof Integer code ? code : 500;

		HttpStatus status = HttpStatus.resolve(rawStatus);
		return status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
	}

	private String resolveMessage(HttpServletRequest request, HttpStatus status) {
		String message = stringAttribute(request, RequestDispatcher.ERROR_MESSAGE);
		if (message == null) {
			return status.getReasonPhrase();
		}
		return message;
	}

	private String stringAttribute(HttpServletRequest request, String name) {
		Object value = request.getAttribute(name);
		if (!(value instanceof String text) || text.isBlank()) {
			return null;
		}
		return text;
	}

	private String header(HttpServletRequest request, String name) {
		String value = request.getHeader(name);
		if (value == null || value.isBlank()) {
			return null;
		}
		return value;
	}

	private void putIfPresent(Map<String, Object> body, String key, String value) {
		if (value != null) {
			body.put(key, value);
		}
	}
}
