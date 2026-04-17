package com.authservice.app.domain.auth.support;

import java.util.Locale;
import java.util.UUID;

public final class Uuid32 {

	private Uuid32() {
	}

	public static String generate() {
		return fromUuid(UUID.randomUUID());
	}

	public static String fromUuid(UUID value) {
		if (value == null) {
			return null;
		}
		return value.toString().replace("-", "");
	}

	public static UUID toUuid(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim().toLowerCase(Locale.ROOT);
		if (normalized.length() == 36) {
			return UUID.fromString(normalized);
		}
		if (normalized.length() != 32) {
			throw new IllegalArgumentException("UUID32 must be 32 lowercase hexadecimal characters.");
		}
		return UUID.fromString(
			normalized.substring(0, 8) + "-"
				+ normalized.substring(8, 12) + "-"
				+ normalized.substring(12, 16) + "-"
				+ normalized.substring(16, 20) + "-"
				+ normalized.substring(20)
		);
	}
}
