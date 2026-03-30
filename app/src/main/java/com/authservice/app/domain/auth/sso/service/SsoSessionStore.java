package com.authservice.app.domain.auth.sso.service;

import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoSessionPayload;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoStatePayload;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoTicketPayload;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class SsoSessionStore {
	private static final Logger log = LoggerFactory.getLogger(SsoSessionStore.class);

	private static final String STATE_PREFIX = "auth:oauth-state:";
	private static final String LEGACY_STATE_PREFIX = "oauth:state:";
	private static final String TICKET_PREFIX = "auth:ticket:";
	private static final String SESSION_PREFIX = "auth:session:";
	private static final String LEGACY_SESSION_PREFIX = "sso:session:";

	private final RedisTemplate<String, Object> redisTemplate;

	public SsoSessionStore(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void saveState(String state, SsoStatePayload payload, Instant expiresAt) {
		save(STATE_PREFIX + state, payload, expiresAt);
	}

	public Optional<SsoStatePayload> consumeState(String state) {
		return consume(new String[] {STATE_PREFIX + state, LEGACY_STATE_PREFIX + state}, SsoStatePayload.class);
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
		return find(new String[] {SESSION_PREFIX + sessionId, LEGACY_SESSION_PREFIX + sessionId}, SsoSessionPayload.class);
	}

	public void revokeSession(String sessionId) {
		delete(SESSION_PREFIX + sessionId);
		delete(LEGACY_SESSION_PREFIX + sessionId);
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
			log.warn("Redis write failed for key={}. Ignoring write and continuing.", key, ex);
		}
	}

	private <T> Optional<T> consume(String[] keys, Class<T> type) {
		Optional<T> value = find(keys, type);
		for (String key : keys) {
			delete(key);
		}
		return value;
	}

	private <T> Optional<T> find(String[] keys, Class<T> type) {
		for (String key : keys) {
			Optional<T> value = find(key, type);
			if (value.isPresent()) {
				return value;
			}
		}
		return Optional.empty();
	}

	private <T> Optional<T> find(String key, Class<T> type) {
		try {
			Object value = redisTemplate.opsForValue().get(key);
			if (type.isInstance(value)) {
				return Optional.of(type.cast(value));
			}
			return Optional.empty();
		} catch (RuntimeException ex) {
			log.warn("Redis read failed for key={}. Returning cache miss.", key, ex);
			return Optional.empty();
		}
	}

	private void delete(String key) {
		try {
			redisTemplate.delete(key);
		} catch (RuntimeException ex) {
			log.warn("Redis delete failed for key={}. Ignoring delete and continuing.", key, ex);
		}
	}
}
