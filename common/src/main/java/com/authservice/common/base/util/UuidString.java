package com.authservice.common.base.util;

import java.util.UUID;

public final class UuidString {

	private UuidString() {}

	public static String generate() {
		return fromUuid(UUID.randomUUID());
	}

	public static String fromUuid(UUID value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}

	public static UUID toUuid(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		if (normalized.length() != 36) {
			throw new IllegalArgumentException("UUID must be canonical CHAR(36).");
		}
		return UUID.fromString(normalized);
	}
}
