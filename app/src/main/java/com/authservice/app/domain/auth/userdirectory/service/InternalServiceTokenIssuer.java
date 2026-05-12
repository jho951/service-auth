package com.authservice.app.domain.auth.userdirectory.service;

import com.authservice.app.domain.auth.userdirectory.config.UserServiceProperties;
import com.authservice.common.base.constant.ErrorCode;
import com.authservice.common.base.exception.GlobalException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class InternalServiceTokenIssuer {

	private final UserServiceProperties userServiceProperties;
	private final Key internalJwtKey;

	public InternalServiceTokenIssuer(UserServiceProperties userServiceProperties) {
		this.userServiceProperties = userServiceProperties;
		this.internalJwtKey = buildInternalJwtKey(userServiceProperties);
	}

	public String issueBearerToken() {
		return "Bearer " + issueToken();
	}

	private String issueToken() {
		UserServiceProperties.Jwt jwt = userServiceProperties.getJwt();
		if (jwt == null || jwt.getSecret() == null || jwt.getSecret().isBlank()) {
			throw new GlobalException(ErrorCode.USER_SERVICE_UNAVAILABLE);
		}

		Date issuedAt = new Date();
		Date expiration = new Date(issuedAt.getTime() + (Math.max(jwt.getTtlSeconds(), 1) * 1000L));
		return Jwts.builder()
			.setIssuer(jwt.getIssuer())
			.setAudience(jwt.getAudience())
			.setSubject(jwt.getSubject())
			.claim("scope", jwt.getScope())
			.setIssuedAt(issuedAt)
			.setExpiration(expiration)
			.signWith(internalJwtKey, SignatureAlgorithm.HS256)
			.compact();
	}

	private static Key buildInternalJwtKey(UserServiceProperties properties) {
		UserServiceProperties.Jwt jwt = properties.getJwt();
		if (jwt == null || jwt.getSecret() == null || jwt.getSecret().isBlank()) {
			return null;
		}
		byte[] secretBytes = jwt.getSecret().getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < 32) {
			throw new GlobalException(ErrorCode.USER_SERVICE_UNAVAILABLE);
		}
		return Keys.hmacShaKeyFor(secretBytes);
	}
}
