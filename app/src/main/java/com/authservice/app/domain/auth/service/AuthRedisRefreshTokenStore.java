package com.authservice.app.domain.auth.service;

import com.auth.spi.RefreshTokenStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuthRedisRefreshTokenStore implements RefreshTokenStore {

	private static final String KEY_PREFIX = "auth:refresh-token:";

	private final RedisTemplate<String, Object> redisTemplate;
	private final Map<String, Instant> fallbackStore = new ConcurrentHashMap<>();

	public AuthRedisRefreshTokenStore(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void save(String userId, String refreshToken, Instant expiresAt) {
		String key = key(userId, refreshToken);
		Duration ttl = Duration.between(Instant.now(), expiresAt);
		if (ttl.isNegative() || ttl.isZero()) {
			deleteKey(key);
			return;
		}
		try {
			redisTemplate.opsForValue().set(key, userId, ttl);
		} catch (RuntimeException ex) {
			fallbackStore.put(key, expiresAt);
		}
	}

	@Override
	public boolean exists(String userId, String refreshToken) {
		String key = key(userId, refreshToken);
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
		deleteKey(key(userId, refreshToken));
	}

	private String key(String userId, String refreshToken) {
		return KEY_PREFIX + userId + ":" + refreshToken;
	}

	private void deleteKey(String key) {
		try {
			redisTemplate.delete(key);
		} catch (RuntimeException ex) {
			fallbackStore.remove(key);
		}
	}
}
