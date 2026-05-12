package com.authservice.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SensitiveDataMaskerTest {

	@Test
	void returnsUnknownForNullOrBlankInput() {
		assertEquals("unknown", SensitiveDataMasker.maskIdentifier(null));
		assertEquals("unknown", SensitiveDataMasker.maskIdentifier("   "));
	}

	@Test
	void masksEmailAddress() {
		assertEquals("h***@gmail.com", SensitiveDataMasker.maskIdentifier("haehak@gmail.com"));
	}

	@Test
	void masksShortIdentifierFully() {
		assertEquals("***", SensitiveDataMasker.maskIdentifier("it"));
	}

	@Test
	void masksGeneralIdentifierLeavingFirstAndLastCharacters() {
		assertEquals("j***t", SensitiveDataMasker.maskIdentifier(" justdoit "));
	}
}
