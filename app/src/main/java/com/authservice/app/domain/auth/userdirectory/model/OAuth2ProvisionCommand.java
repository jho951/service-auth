package com.authservice.app.domain.auth.userdirectory.model;

import com.authservice.app.domain.auth.model.OAuthProvider;

public record OAuth2ProvisionCommand(
	OAuthProvider provider,
	String providerUserId,
	String email,
	String name,
	String avatarUrl
) {
}
