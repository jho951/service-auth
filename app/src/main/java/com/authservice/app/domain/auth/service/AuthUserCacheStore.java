package com.authservice.app.domain.auth.service;

import com.authservice.app.domain.auth.model.AuthUser;
import com.authservice.common.redis.support.RedisTypedStore;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthUserCacheStore {

	private static final String USER_CACHE_PREFIX = "auth:user:";

	private final RedisTypedStore redisTypedStore;
	private final Duration ttl;

	public AuthUserCacheStore(
		RedisTypedStore redisTypedStore,
		@Value("${auth.user-cache-ttl-seconds:300}") long ttlSeconds
	) {
		this.redisTypedStore = redisTypedStore;
		this.ttl = Duration.ofSeconds(Math.max(1, ttlSeconds));
	}

	public Optional<AuthUser> get(String username) {
		try {
			return redisTypedStore.find(key(username), CachedAuthUser.class).map(CachedAuthUser::toUser);
		} catch (RuntimeException ex) {
			return Optional.empty();
		}
	}

	public void put(String userId, String username, String passwordHash, List<String> roles) {
		try {
			CachedAuthUser value = new CachedAuthUser(userId, username, passwordHash, roles);
			redisTypedStore.save(key(username), value, ttl);
		} catch (RuntimeException ex) {
			// ignore redis failures and continue with source-of-truth flow
		}
	}

	public void evict(String username) {
		try {
			redisTypedStore.delete(key(username));
		} catch (RuntimeException ex) {
			// ignore redis failures and continue with source-of-truth flow
		}
	}

	private String key(String username) {
		return USER_CACHE_PREFIX + username;
	}

	private record CachedAuthUser(
		String userId,
		String username,
		String passwordHash,
		List<String> roles
	) {
		private AuthUser toUser() {
			return new AuthUser(userId, username, passwordHash, roles);
		}
	}
}
