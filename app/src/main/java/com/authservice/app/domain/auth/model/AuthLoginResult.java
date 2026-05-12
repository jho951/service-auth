package com.authservice.app.domain.auth.model;

public enum AuthLoginResult {
	SUCCESS,
	FAILURE;

	public String value() {
		return name();
	}
}
