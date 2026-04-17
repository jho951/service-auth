package com.authservice.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.authservice.app.common.base.exception.GlobalException;
import com.authservice.app.domain.auth.config.AuthHttpProperties;
import com.authservice.app.domain.auth.model.AuthPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuthJwtTokenServiceTest {

	private static final String SECRET = "abcdefghijklmnopqrstuvwxyz12345678901234567890";
	private static final Key KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

	@Test
	void accessTokenContainsIssuerAuthService() {
		AuthJwtTokenService tokenService = new AuthJwtTokenService(SECRET, "block-service", 60, 120);
		AuthPrincipal principal = new AuthPrincipal("46c45ce7-d50c-436e-9394-263941839cf7", List.of("USER"), java.util.Map.of());

		String token = tokenService.issueAccessToken(principal);

		Claims claims = Jwts.parserBuilder()
			.setSigningKey(KEY)
			.build()
			.parseClaimsJws(token)
			.getBody();

		assertThat(claims.getIssuer()).isEqualTo("auth-service");
		assertThat(claims.getAudience()).isEqualTo("block-service");
		assertThat(claims.getSubject()).isEqualTo(principal.userId());
		assertThat(claims.get("token_type", String.class)).isEqualTo("access");
		assertThat(claims.get("authorities")).isInstanceOf(List.class);
		assertThat(claims.get("roles")).isInstanceOf(List.class);
	}

	@Test
	void refreshTokenDoesNotNeedIssuerButStillVerifies() {
		AuthJwtTokenService tokenService = new AuthJwtTokenService(SECRET, "block-service", 60, 120);
		AuthPrincipal principal = new AuthPrincipal("46c45ce7-d50c-436e-9394-263941839cf7", List.of("USER"), java.util.Map.of());

		String token = tokenService.issueRefreshToken(principal);

		Claims claims = Jwts.parserBuilder()
			.setSigningKey(KEY)
			.build()
			.parseClaimsJws(token)
			.getBody();

		assertThat(claims.getIssuer()).isNull();
		assertThat(claims.getAudience()).isEqualTo("block-service");
		assertThat(claims.get("token_type", String.class)).isEqualTo("refresh");
		assertThat(tokenService.verifyRefreshToken(token).userId()).isEqualTo(principal.userId());
	}

	@Test
	void tokenServiceConfigFailsFastWithoutExplicitSecret() {
		AuthHttpProperties properties = new AuthHttpProperties();
		properties.getJwt().setSecret("");

		assertThatThrownBy(() -> new AuthJwtTokenServiceConfig().tokenService(properties, "block-service"))
			.isInstanceOf(GlobalException.class);
	}
}
