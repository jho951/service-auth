package com.authservice.common.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.authservice.common.logging.LoggingHeaders;

class ClientIpResolverTest {

	@Test
	void returnsUnknownWhenRequestIsNull() {
		assertEquals("unknown", ClientIpResolver.resolve(null));
	}

	@Test
	void resolvesFirstForwardedIpWhenHeaderExists() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(LoggingHeaders.X_FORWARDED_FOR, " 203.0.113.10, 198.51.100.2 ");

		assertEquals("203.0.113.10", ClientIpResolver.resolve(request));
	}

	@Test
	void fallsBackToRemoteAddressWhenForwardedHeaderMissing() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr("10.0.0.7");

		assertEquals("10.0.0.7", ClientIpResolver.resolve(request));
	}

	@Test
	void returnsUnknownWhenRemoteAddressBlank() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(LoggingHeaders.X_FORWARDED_FOR, "   ");
		request.setRemoteAddr(" ");

		assertEquals("unknown", ClientIpResolver.resolve(request));
	}
}
