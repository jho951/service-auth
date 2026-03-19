package com.authservice.app.domain.auth.service;

import com.auth.spi.RefreshTokenStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuthRedisRefreshTokenStore implements RefreshTokenStore {

	private static final String JTI_PREFIX = "refresh:jti:";
	private static final String USER_PREFIX = "refresh:user:";

	private final RedisTemplate<String, Object> redisTemplate;
	private final Map<String, Instant> fallbackStore = new ConcurrentHashMap<>();

	public AuthRedisRefreshTokenStore(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
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
			redisTemplate.opsForValue().set(tokenKey, metadata, ttl);
			redisTemplate.opsForValue().set(userKey, tokenKey, ttl);
		} catch (RuntimeException ex) {
			fallbackStore.put(tokenKey, expiresAt);
			fallbackStore.put(userKey, expiresAt);
		}
	}

	@Override
	public boolean exists(String userId, String refreshToken) {
		String key = tokenKey(refreshToken);
		try {
			Boolean exists = redisTemplate.hasKey(key);
			return Boolean.TRUE.equals(exists);
		} catch (RuntimeException ex) {
			Instant expiresAt = fallbackStore.get(key);
			if (expiresAt == null) {
				return false;
			}
			if (expiresAt.isBefore(Instant.now())) {
				fallbackStore.remove(key);
				return false;
			}
			return true;
		}
	}

	@Override
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
			redisTemplate.delete(key);
		} catch (RuntimeException ex) {
			fallbackStore.remove(key);
		}
	}

	private record RefreshTokenMetadata(String userId, String tokenHash, String expiresAt) {
	}
}
