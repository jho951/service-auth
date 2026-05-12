package com.authservice.app.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import com.authservice.common.redis.support.RedisTypedStore;
import org.springframework.stereotype.Component;

@Component
public class AuthRedisRefreshTokenStore {

	private static final String JTI_PREFIX = "refresh:jti:";
	private static final String USER_PREFIX = "refresh:user:";

	private final RedisTypedStore redisTypedStore;

	public AuthRedisRefreshTokenStore(RedisTypedStore redisTypedStore) {
		this.redisTypedStore = redisTypedStore;
	}

	public void save(String userId, String refreshToken, Instant expiresAt) {
		String tokenKey = tokenKey(refreshToken);
		String userKey = userKey(userId, refreshToken);
		Duration ttl = Duration.between(Instant.now(), expiresAt);
		if (ttl.isNegative() || ttl.isZero()) {
			deleteKey(tokenKey);
			deleteKey(userKey);
			return;
			}
			try {
				RefreshTokenMetadata metadata = new RefreshTokenMetadata(userId, hash(refreshToken), expiresAt.toString());
				redisTypedStore.save(tokenKey, metadata, ttl);
				redisTypedStore.save(userKey, tokenKey, ttl);
			} catch (RuntimeException ex) {
				// keep auth flow available when Redis is unavailable
			}
	}

	public boolean exists(String userId, String refreshToken) {
		String key = tokenKey(refreshToken);
		try {
			return redisTypedStore.hasKey(key);
		} catch (RuntimeException ex) {
			return false;
		}
	}

	public void revoke(String userId, String refreshToken) {
		deleteKey(tokenKey(refreshToken));
		deleteKey(userKey(userId, refreshToken));
	}

	private String tokenKey(String refreshToken) {
		return JTI_PREFIX + hash(refreshToken);
	}

	private String userKey(String userId, String refreshToken) {
		return USER_PREFIX + userId + ":" + hash(refreshToken);
	}

	private String hash(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder();
			for (byte b : bytes) {
				builder.append(String.format("%02x", b));
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}

	private void deleteKey(String key) {
		try {
			redisTypedStore.delete(key);
		} catch (RuntimeException ex) {
			// keep auth flow available when Redis is unavailable
		}
	}

	private record RefreshTokenMetadata(String userId, String tokenHash, String expiresAt) {
	}
}
