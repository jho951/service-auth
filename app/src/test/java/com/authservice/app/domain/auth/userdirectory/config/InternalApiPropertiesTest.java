package com.authservice.app.domain.auth.userdirectory.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.authservice.common.base.exception.GlobalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InternalApiPropertiesTest {

	private InternalApiProperties internalApiProperties;

	@BeforeEach
	void setUp() {
		internalApiProperties = new InternalApiProperties();
		internalApiProperties.setKey("test-internal-key");
	}

	@Test
	void allowsWhenAuthorizationHeaderMatches() {
		assertDoesNotThrow(() ->
			internalApiProperties.validateInternalAccess("Bearer test-internal-key", null)
		);
	}

	@Test
	void allowsWhenInternalSecretHeaderMatches() {
		assertDoesNotThrow(() ->
			internalApiProperties.validateInternalAccess(null, "test-internal-key")
		);
	}

	@Test
	void rejectsWhenBothHeadersAreInvalid() {
		assertThrows(GlobalException.class, () ->
			internalApiProperties.validateInternalAccess("Bearer wrong-key", "wrong-key")
		);
	}
}
