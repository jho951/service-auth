package com.authservice.app.domain.auth.model;

import java.util.Arrays;

public enum OAuthProvider {
	GITHUB("github", "GITHUB");

	private final String externalName;
	private final String socialType;

	OAuthProvider(String externalName, String socialType) {
		this.externalName = externalName;
		this.socialType = socialType;
	}

	public String externalName() {
		return externalName;
	}

	public String socialType() {
		return socialType;
	}

	public static OAuthProvider fromExternalName(String value) {
		return Arrays.stream(values())
			.filter(provider -> provider.externalName.equalsIgnoreCase(value))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unsupported OAuth provider: " + value));
	}
}
