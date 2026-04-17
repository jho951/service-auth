package com.authservice.app.domain.auth.model;

import java.util.List;
import java.util.Map;

public record AuthPrincipal(
	String userId,
	List<String> roles,
	Map<String, Object> attributes
) {
	public AuthPrincipal {
		roles = roles == null ? List.of() : List.copyOf(roles);
		attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
	}
}
