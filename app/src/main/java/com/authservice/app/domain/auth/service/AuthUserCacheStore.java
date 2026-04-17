package com.authservice.app.domain.auth.service;

import com.authservice.app.domain.auth.model.AuthUser;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuthUserCacheStore {

	private static final String USER_CACHE_PREFIX = "auth:user:";

	private final RedisTemplate<String, Object> redisTemplate;
	private final Duration ttl;

	public AuthUserCacheStore(
		RedisTemplate<String, Object> redisTemplate,
		@Value("${auth.user-cache-ttl-seconds:300}") long ttlSeconds
	) {
		this.redisTemplate = redisTemplate;
		this.ttl = Duration.ofSeconds(Math.max(1, ttlSeconds));
	}

	public Optional<AuthUser> get(String username) {
		try {
			Object cached = redisTemplate.opsForValue().get(key(username));
			if (cached instanceof CachedAuthUser value) {
				return Optional.of(value.toUser());
			}
			if (cached instanceof Map<?, ?> map) {
				return mapToUser(map);
			}
			return Optional.empty();
		} catch (RuntimeException ex) {
			return Optional.empty();
		}
	}

	public void put(String userId, String username, String passwordHash, List<String> roles) {
		try {
			CachedAuthUser value = new CachedAuthUser(userId, username, passwordHash, roles);
			redisTemplate.opsForValue().set(key(username), value, ttl);
		} catch (RuntimeException ex) {
			// ignore redis failures and continue with source-of-truth flow
		}
	}

	public void evict(String username) {
		try {
			redisTemplate.delete(key(username));
		} catch (RuntimeException ex) {
			// ignore redis failures and continue with source-of-truth flow
		}
	}

	private String key(String username) {
		return USER_CACHE_PREFIX + username;
	}

	@SuppressWarnings("unchecked")
	private Optional<AuthUser> mapToUser(Map<?, ?> map) {
		Object userId = map.get("userId");
		Object username = map.get("username");
		Object password = map.containsKey("passwordHash") ? map.get("passwordHash") : map.get("password");
		Object rolesRaw = map.get("roles");
		if (!(userId instanceof String)
			|| !(username instanceof String)
			|| !(password instanceof String)
			|| !(rolesRaw instanceof List<?> rolesList)) {
			return Optional.empty();
		}
		List<String> roles = rolesList.stream()
			.filter(String.class::isInstance)
			.map(String.class::cast)
			.toList();
		if (roles.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new AuthUser((String) userId, (String) username, (String) password, roles));
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
