package com.authservice.app.security;

import com.authservice.app.common.base.constant.ErrorCode;
import com.authservice.app.common.base.exception.GlobalException;
import com.authservice.app.domain.auth.model.AuthPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** auth-service의 JWT 발급 구현체입니다. */
public class AuthJwtTokenService {

	private static final String KEY_ISSUER = "iss";
	private static final String KEY_AUDIENCE = "aud";
	private static final String KEY_TOKEN_TYPE = "token_type";
	private static final String KEY_AUTHORITIES = "authorities";
	private static final String KEY_ROLES = "roles";
	private static final String TOKEN_TYPE_ACCESS = "access";
	private static final String TOKEN_TYPE_REFRESH = "refresh";
	private static final String ISSUER = "auth-service";

	private final Key key;
	private final String audience;
	private final long accessSeconds;
	private final long refreshSeconds;

	public AuthJwtTokenService(String secret, String audience, long accessSeconds, long refreshSeconds) {
		if (secret == null || secret.isBlank()) {
			throw new GlobalException(ErrorCode.INVALID_REQUEST);
		}
		if (audience == null || audience.isBlank()) {
			throw new GlobalException(ErrorCode.INVALID_REQUEST);
		}
		byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < 32) {
			throw new GlobalException(ErrorCode.INVALID_REQUEST);
		}
		this.key = Keys.hmacShaKeyFor(secretBytes);
		this.audience = audience;
		this.accessSeconds = accessSeconds;
		this.refreshSeconds = refreshSeconds;
	}

	public String issueAccessToken(AuthPrincipal principal) {
		return buildToken(principal, accessSeconds, TOKEN_TYPE_ACCESS, true);
	}

	public String issueRefreshToken(AuthPrincipal principal) {
		return buildToken(principal, refreshSeconds, TOKEN_TYPE_REFRESH, false);
	}

	public AuthPrincipal verifyAccessToken(String token) {
		return parseAndToPrincipal(token, TOKEN_TYPE_ACCESS);
	}

	public AuthPrincipal verifyRefreshToken(String token) {
		return parseAndToPrincipal(token, TOKEN_TYPE_REFRESH);
	}

	private String buildToken(AuthPrincipal principal, long ttlSeconds, String tokenType, boolean includeIssuer) {
		Date issuedAt = new Date();
		Date expiration = new Date(issuedAt.getTime() + (Math.max(ttlSeconds, 1) * 1000L));

		Map<String, Object> claims = new HashMap<>(principal.attributes());
		claims.remove(KEY_ISSUER);
		claims.remove(KEY_AUDIENCE);
		if (!principal.roles().isEmpty()) {
			claims.put(KEY_AUTHORITIES, principal.roles());
			claims.put(KEY_ROLES, principal.roles());
		}

		JwtBuilder builder = Jwts.builder()
			.setSubject(principal.userId())
			.setAudience(audience)
			.addClaims(claims)
			.claim(KEY_TOKEN_TYPE, tokenType)
			.setIssuedAt(issuedAt)
			.setExpiration(expiration);

		if (includeIssuer) {
			builder.setIssuer(ISSUER);
		}

		return builder.signWith(key, SignatureAlgorithm.HS256).compact();
	}

	private AuthPrincipal parseAndToPrincipal(String token, String expectedTokenType) {
		try {
			JwtParser parser = parserBuilder().build();
			Claims claims = parser.parseClaimsJws(token).getBody();

			String tokenType = claims.get(KEY_TOKEN_TYPE, String.class);
			if (tokenType == null || !tokenType.equals(expectedTokenType)) {
				throw new GlobalException(ErrorCode.INVALID_TOKEN);
			}

			String userId = claims.getSubject();
			Map<String, Object> attributes = new HashMap<>(claims);
			attributes.remove("sub");
			attributes.remove("iat");
			attributes.remove("exp");
			attributes.remove(KEY_TOKEN_TYPE);
			attributes.remove(KEY_AUTHORITIES);
			attributes.remove(KEY_ROLES);
			attributes.remove(KEY_ISSUER);
			attributes.remove(KEY_AUDIENCE);

			List<String> authorities = toAuthorities(claims.get(KEY_AUTHORITIES, Object.class));
			if (authorities.isEmpty()) {
				authorities = toAuthorities(claims.get(KEY_ROLES, Object.class));
			}

			return new AuthPrincipal(userId, authorities, attributes);
		} catch (GlobalException ex) {
			throw ex;
		} catch (JwtException | IllegalArgumentException ex) {
			throw new GlobalException(ErrorCode.INVALID_TOKEN);
		}
	}

	private JwtParserBuilder parserBuilder() {
		return Jwts.parserBuilder().setSigningKey(key);
	}

	private List<String> toAuthorities(Object value) {
		if (value instanceof String string) return List.of(string);
		if (value instanceof List<?> list) {
			return list.stream()
				.filter(String.class::isInstance)
				.map(String.class::cast)
				.toList();
		}
		return List.of();
	}
}
