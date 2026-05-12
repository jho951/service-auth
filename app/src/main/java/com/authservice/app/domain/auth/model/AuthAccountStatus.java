package com.authservice.app.domain.auth.model;

public enum AuthAccountStatus {
	ACTIVE("A");

	private final String code;

	AuthAccountStatus(String code) {
		this.code = code;
	}

	public String code() {
		return code;
	}

	public boolean matches(String value) {
		return value != null && code.equalsIgnoreCase(value);
	}

	public static boolean isActive(String value) {
		return ACTIVE.matches(value);
	}

	public static String normalizeOrDefault(String value) {
		return value == null || value.isBlank() ? ACTIVE.code : value;
	}
}
