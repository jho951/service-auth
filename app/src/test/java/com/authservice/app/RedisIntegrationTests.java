package com.authservice.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.authservice.app.domain.auth.service.AuthRedisRefreshTokenStore;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoSessionPayload;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoStatePayload;
import com.authservice.app.domain.auth.sso.service.SsoSessionStore;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_INTEGRATION_TESTS", matches = "true")
class RedisIntegrationTests {

	@Autowired
	private SsoSessionStore ssoSessionStore;

	@Autowired
	private AuthRedisRefreshTokenStore refreshTokenStore;

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	@Test
	void ssoSessionStoreReadsAndDeletesCentralRedisKeys() {
		String state = UUID.randomUUID().toString();
		String sessionId = UUID.randomUUID().toString();
		Instant expiresAt = Instant.now().plusSeconds(300);

		ssoSessionStore.saveState(state, new SsoStatePayload("http://localhost/callback", "EDITOR", expiresAt), expiresAt);
		ssoSessionStore.saveSession(
			sessionId,
			new SsoSessionPayload("user-1", "user@example.com", "User", null, List.of("USER"), expiresAt),
			expiresAt
		);

		assertThat(redisTemplate.hasKey("auth:oauth-state:" + state)).isTrue();
		assertThat(redisTemplate.hasKey("auth:session:" + sessionId)).isTrue();
		assertThat(ssoSessionStore.consumeState(state)).isPresent();
		assertThat(ssoSessionStore.findSession(sessionId)).isPresent();

		ssoSessionStore.revokeSession(sessionId);

		assertThat(redisTemplate.hasKey("auth:oauth-state:" + state)).isFalse();
		assertThat(redisTemplate.hasKey("auth:session:" + sessionId)).isFalse();
	}

	@Test
	void refreshTokenStoreSavesHashedKeysWithTtlAndSupportsRevocation() {
		String userId = "user-" + UUID.randomUUID();
		String refreshToken = "refresh-" + UUID.randomUUID();
		Instant expiresAt = Instant.now().plusSeconds(300);
		String tokenHash = (String) ReflectionTestUtils.invokeMethod(refreshTokenStore, "hash", refreshToken);
		String tokenKey = "refresh:jti:" + tokenHash;
		String userKey = "refresh:user:" + userId + ":" + tokenHash;

		refreshTokenStore.save(userId, refreshToken, expiresAt);

		assertThat(redisTemplate.hasKey(tokenKey)).isTrue();
		assertThat(redisTemplate.hasKey(userKey)).isTrue();
		assertThat(refreshTokenStore.exists(userId, refreshToken)).isTrue();
		Long tokenTtl = redisTemplate.getExpire(tokenKey, TimeUnit.SECONDS);
		assertThat(tokenTtl).isNotNull();
		assertThat(tokenTtl).isPositive();
		assertThat(tokenTtl).isLessThanOrEqualTo(300L);

		refreshTokenStore.revoke(userId, refreshToken);

		assertThat(redisTemplate.hasKey(tokenKey)).isFalse();
		assertThat(redisTemplate.hasKey(userKey)).isFalse();
		assertThat(refreshTokenStore.exists(userId, refreshToken)).isFalse();
	}
}
