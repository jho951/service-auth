package com.authservice.app.security.jwt;

import com.authservice.common.base.constant.ErrorCode;
import com.authservice.common.base.exception.GlobalException;
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

/**
 * auth-service 전용 JWT 발급 및 검증을 담당하는 서비스입니다.
 * 액세스 토큰과 리프레시 토큰의 생성, 만료 시간 반영, 기본 claim 정규화를 함께 수행합니다.
 */
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

	/**
	 * JWT 서명 키와 토큰 만료 정책을 초기화합니다.
	 *
	 * @param secret HMAC 서명에 사용할 비밀키
	 * @param audience 토큰 audience claim 값
	 * @param accessSeconds 액세스 토큰 만료 시간(초)
	 * @param refreshSeconds 리프레시 토큰 만료 시간(초)
	 */
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

	/**
	 * 인증 주체를 기반으로 액세스 토큰을 발급합니다.
	 *
	 * @param principal 토큰 발급 대상 주체
	 * @return 서명된 액세스 토큰 문자열
	 */
	public String issueAccessToken(AuthPrincipal principal) {
		return buildToken(principal, accessSeconds, TOKEN_TYPE_ACCESS, true);
	}

	/**
	 * 인증 주체를 기반으로 리프레시 토큰을 발급합니다.
	 *
	 * @param principal 토큰 발급 대상 주체
	 * @return 서명된 리프레시 토큰 문자열
	 */
	public String issueRefreshToken(AuthPrincipal principal) {
		return buildToken(principal, refreshSeconds, TOKEN_TYPE_REFRESH, false);
	}

	/**
	 * 액세스 토큰을 검증하고 애플리케이션 인증 주체로 변환합니다.
	 *
	 * @param token 검증할 액세스 토큰
	 * @return 토큰에서 복원한 인증 주체
	 */
	public AuthPrincipal verifyAccessToken(String token) {
		return parseAndToPrincipal(token, TOKEN_TYPE_ACCESS);
	}

	/**
	 * 리프레시 토큰을 검증하고 애플리케이션 인증 주체로 변환합니다.
	 *
	 * @param token 검증할 리프레시 토큰
	 * @return 토큰에서 복원한 인증 주체
	 */
	public AuthPrincipal verifyRefreshToken(String token) {
		return parseAndToPrincipal(token, TOKEN_TYPE_REFRESH);
	}

	/**
	 * 리프레시 토큰의 만료 시간(초)을 반환합니다.
	 *
	 * @return 리프레시 토큰 TTL(초)
	 */
	public long refreshSeconds() {
		return refreshSeconds;
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

		if (includeIssuer) builder.setIssuer(ISSUER);

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
