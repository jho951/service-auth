package com.authservice.app.security;

import com.auth.api.model.Principal;
import com.auth.session.SessionPrincipalMapper;
import com.auth.session.SessionStore;
import com.auth.spi.TokenService;
import com.authservice.app.domain.auth.model.AuthPrincipal;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoSessionPayload;
import com.authservice.app.domain.auth.sso.service.SsoSessionStore;
import io.github.jho951.platform.security.auth.PlatformSecurityContextResolvers;
import io.github.jho951.platform.security.api.SecurityContextResolver;
import io.github.jho951.platform.security.policy.PlatformSecurityProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class PlatformSecurityAuthConfig {

	@Bean
	@Primary
	public SecurityContextResolver securityContextResolver(
		TokenService platformTokenService,
		SessionStore platformSessionStore,
		SessionPrincipalMapper platformSessionPrincipalMapper
	) {
		return PlatformSecurityContextResolvers.hybrid(
			platformTokenService,
			platformSessionStore,
			platformSessionPrincipalMapper
		);
	}

	@Bean
	public TokenService platformTokenService(AuthJwtTokenService tokenService) {
		return new TokenService() {
			@Override
			public String issueAccessToken(Principal principal) {
				return tokenService.issueAccessToken(toAuthPrincipal(principal));
			}

			@Override
			public String issueRefreshToken(Principal principal) {
				return tokenService.issueRefreshToken(toAuthPrincipal(principal));
			}

			@Override
			public Principal verifyAccessToken(String token) {
				return toPlatformPrincipal(tokenService.verifyAccessToken(token));
			}

			@Override
			public Principal verifyRefreshToken(String token) {
				return toPlatformPrincipal(tokenService.verifyRefreshToken(token));
			}
		};
	}

	@Bean
	public SessionStore platformSessionStore(
		SsoSessionStore ssoSessionStore,
		PlatformSecurityProperties platformSecurityProperties
	) {
		return new SessionStore() {
			@Override
			public void save(String sessionId, Principal principal) {
				Instant expiresAt = Instant.now().plus(sessionTtl(platformSecurityProperties));
				Map<String, Object> attributes = principal.getAttributes();
				ssoSessionStore.saveSession(
					sessionId,
					new SsoSessionPayload(
						principal.getUserId(),
						stringAttribute(attributes, "email"),
						stringAttribute(attributes, "name"),
						stringAttribute(attributes, "avatarUrl"),
						principal.getAuthorities(),
						stringAttribute(attributes, "status"),
						expiresAt
					),
					expiresAt
				);
			}

			@Override
			public Optional<Principal> find(String sessionId) {
				return ssoSessionStore.findSession(sessionId).map(PlatformSecurityAuthConfig::toPlatformPrincipal);
			}

			@Override
			public void revoke(String sessionId) {
				ssoSessionStore.revokeSession(sessionId);
			}
		};
	}

	@Bean
	public SessionPrincipalMapper platformSessionPrincipalMapper() {
		return (sessionId, principal, attributes) -> principal;
	}

	private static Principal toPlatformPrincipal(AuthPrincipal principal) {
		return new Principal(principal.userId(), principal.roles(), new LinkedHashMap<>(principal.attributes()));
	}

	private static Principal toPlatformPrincipal(SsoSessionPayload payload) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		putIfPresent(attributes, "email", payload.getEmail());
		putIfPresent(attributes, "name", payload.getName());
		putIfPresent(attributes, "avatarUrl", payload.getAvatarUrl());
		putIfPresent(attributes, "status", payload.getStatus());
		return new Principal(payload.getUserId(), listOrEmpty(payload.getRoles()), attributes);
	}

	private static AuthPrincipal toAuthPrincipal(Principal principal) {
		return new AuthPrincipal(
			principal.getUserId(),
			principal.getAuthorities(),
			new LinkedHashMap<>(principal.getAttributes())
		);
	}

	private static Duration sessionTtl(PlatformSecurityProperties properties) {
		Duration ttl = properties.getAuth().getRefreshTokenTtl();
		if (ttl == null || ttl.isNegative() || ttl.isZero()) {
			return Duration.ofHours(8);
		}
		return ttl;
	}

	private static List<String> listOrEmpty(List<String> values) {
		return values == null ? List.of() : values;
	}

	private static String stringAttribute(Map<String, Object> attributes, String key) {
		Object value = attributes.get(key);
		if (value instanceof String text && !text.isBlank()) {
			return text;
		}
		return null;
	}

	private static void putIfPresent(Map<String, Object> values, String key, String value) {
		if (value != null && !value.isBlank()) {
			values.putIfAbsent(key, value);
		}
	}
}
