package com.authservice.app.domain.auth.model;

public enum AuthFailureReason {
	AUTH_FAILURE("AUTH_FAILURE"),
	INVALID_CREDENTIALS_OR_POLICY("INVALID_CREDENTIALS_OR_POLICY");

	private final String code;

	AuthFailureReason(String code) {
		this.code = code;
	}

	public String code() {
		return code;
	}
}
