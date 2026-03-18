package com.authservice.app.domain.auth.sso.service;

import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoSessionPayload;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoStatePayload;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoTicketPayload;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class SsoSessionStore {

	private static final String STATE_PREFIX = "auth:oauth-state:";
	private static final String TICKET_PREFIX = "auth:ticket:";
	private static final String SESSION_PREFIX = "auth:session:";

	private final RedisTemplate<String, Object> redisTemplate;
	private final Map<String, ExpiringValue> fallbackStore = new ConcurrentHashMap<>();

	public SsoSessionStore(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void saveState(String state, SsoStatePayload payload, Instant expiresAt) {
		save(STATE_PREFIX + state, payload, expiresAt);
	}

	public Optional<SsoStatePayload> consumeState(String state) {
		return consume(STATE_PREFIX + state, SsoStatePayload.class);
	}

	public void saveTicket(String ticket, SsoTicketPayload payload, Instant expiresAt) {
		save(TICKET_PREFIX + ticket, payload, expiresAt);
	}

	public Optional<SsoTicketPayload> consumeTicket(String ticket) {
		return consume(TICKET_PREFIX + ticket, SsoTicketPayload.class);
	}

	public void saveSession(String sessionId, SsoSessionPayload payload, Instant expiresAt) {
		save(SESSION_PREFIX + sessionId, payload, expiresAt);
	}

	public Optional<SsoSessionPayload> findSession(String sessionId) {
		return find(SESSION_PREFIX + sessionId, SsoSessionPayload.class);
	}

	public void revokeSession(String sessionId) {
		delete(SESSION_PREFIX + sessionId);
	}

	private void save(String key, Object payload, Instant expiresAt) {
		Duration ttl = Duration.between(Instant.now(), expiresAt);
		if (ttl.isNegative() || ttl.isZero()) {
			delete(key);
			return;
		}
		try {
			redisTemplate.opsForValue().set(key, payload, ttl);
		} catch (RuntimeException ex) {
			fallbackStore.put(key, new ExpiringValue(payload, expiresAt));
		}
	}

	private <T> Optional<T> consume(String key, Class<T> type) {
		Optional<T> value = find(key, type);
		delete(key);
		return value;
	}

	private <T> Optional<T> find(String key, Class<T> type) {
		try {
			Object value = redisTemplate.opsForValue().get(key);
			if (type.isInstance(value)) {
				return Optional.of(type.cast(value));
			}
			return findFallback(key, type);
		} catch (RuntimeException ex) {
			return findFallback(key, type);
		}
	}

	private void delete(String key) {
		try {
			redisTemplate.delete(key);
		} catch (RuntimeException ex) {
			fallbackStore.remove(key);
		}
	}

	private <T> Optional<T> findFallback(String key, Class<T> type) {
		ExpiringValue fallback = fallbackStore.get(key);
		if (fallback == null || fallback.expiresAt().isBefore(Instant.now())) {
			fallbackStore.remove(key);
			return Optional.empty();
		}

		Object value = fallback.value();
		if (type.isInstance(value)) {
			return Optional.of(type.cast(value));
		}

		return Optional.empty();
	}

	private record ExpiringValue(Object value, Instant expiresAt) {
	}
}
