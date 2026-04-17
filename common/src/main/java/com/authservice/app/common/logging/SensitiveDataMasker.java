package com.authservice.app.common.logging;

public final class SensitiveDataMasker {

	private SensitiveDataMasker() {
	}

	public static String maskIdentifier(String value) {
		if (value == null || value.isBlank()) {
			return "unknown";
		}
		String trimmed = value.trim();
		int atIndex = trimmed.indexOf('@');
		if (atIndex > 1) {
			return trimmed.charAt(0) + "***" + trimmed.substring(atIndex);
		}
		if (trimmed.length() <= 2) {
			return "***";
		}
		return trimmed.charAt(0) + "***" + trimmed.charAt(trimmed.length() - 1);
	}
}
