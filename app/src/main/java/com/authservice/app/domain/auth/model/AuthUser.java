package com.authservice.app.domain.auth.model;

import java.util.List;

public record AuthUser(
	String userId,
	String username,
	String passwordHash,
	List<String> roles
) {
	public AuthUser {
		roles = roles == null ? List.of() : List.copyOf(roles);
	}
}
