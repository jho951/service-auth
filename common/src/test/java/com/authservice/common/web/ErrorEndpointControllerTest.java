package com.authservice.common.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import jakarta.servlet.RequestDispatcher;

class ErrorEndpointControllerTest {

	private final ErrorEndpointController controller = new ErrorEndpointController();

	@Test
	void buildsErrorResponseFromRequestAttributes() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
		request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "missing");
		request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/api/users/1");
		request.addHeader("X-Request-Id", "req-1");
		request.addHeader("X-Correlation-Id", "corr-1");

		ResponseEntity<Map<String, Object>> response = controller.error(request);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
		assertNotNull(response.getBody());
		assertEquals(404, response.getBody().get("status"));
		assertEquals("Not Found", response.getBody().get("error"));
		assertEquals("missing", response.getBody().get("message"));
		assertEquals("/api/users/1", response.getBody().get("path"));
		assertEquals("req-1", response.getBody().get("requestId"));
		assertEquals("corr-1", response.getBody().get("correlationId"));
	}

	@Test
	void fallsBackToInternalServerErrorDefaults() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, "bad-status");
		request.setAttribute(RequestDispatcher.ERROR_MESSAGE, " ");

		ResponseEntity<Map<String, Object>> response = controller.error(request);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(500, response.getBody().get("status"));
		assertEquals("Internal Server Error", response.getBody().get("error"));
		assertEquals("Internal Server Error", response.getBody().get("message"));
		assertFalse(response.getBody().containsKey("path"));
		assertFalse(response.getBody().containsKey("requestId"));
		assertFalse(response.getBody().containsKey("correlationId"));
	}
}
