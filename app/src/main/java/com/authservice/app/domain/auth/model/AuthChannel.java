package com.authservice.app.domain.auth.model;

public enum AuthChannel {
	PASSWORD,
	SSO;

	public String value() {
		return name();
	}
}
