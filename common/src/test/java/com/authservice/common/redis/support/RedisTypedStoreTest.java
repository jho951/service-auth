package com.authservice.common.redis.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RedisTypedStoreTest {

	@Mock
	private RedisTemplate<String, Object> redisTemplate;

	private ValueOperations<String, Object> valueOperations;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private RedisTypedStore store;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		valueOperations = mock(ValueOperations.class);
		store = new RedisTypedStore(redisTemplate, objectMapper);
	}

	@Test
	void saveDelegatesToRedisValueOperations() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		store.save("token:1", "value", Duration.ofMinutes(5));

		verify(valueOperations).set("token:1", "value", Duration.ofMinutes(5));
	}

	@Test
	void hasKeyReturnsTrueOnlyForExplicitTrue() {
		when(redisTemplate.hasKey("exists")).thenReturn(true);
		when(redisTemplate.hasKey("missing")).thenReturn(false);
		when(redisTemplate.hasKey("unknown")).thenReturn(null);

		assertTrue(store.hasKey("exists"));
		assertFalse(store.hasKey("missing"));
		assertFalse(store.hasKey("unknown"));
	}

	@Test
	void deleteDelegatesToRedisTemplate() {
		store.delete("token:1");

		verify(redisTemplate).delete("token:1");
	}

	@Test
	void findReturnsEmptyWhenNoValueExists() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("missing")).thenReturn(null);

		assertTrue(store.find("missing", Payload.class).isEmpty());
	}

	@Test
	void findReturnsCastedValueWhenTypeAlreadyMatches() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		Payload payload = new Payload();
		payload.name = "alice";
		when(valueOperations.get("payload")).thenReturn(payload);

		Optional<Payload> result = store.find("payload", Payload.class);

		assertTrue(result.isPresent());
		assertSame(payload, result.get());
	}

	@Test
	void findConvertsValueWhenStoredTypeDiffers() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("payload")).thenReturn(Map.of("name", "alice"));

		Optional<Payload> result = store.find("payload", Payload.class);

		assertTrue(result.isPresent());
		assertEquals("alice", result.get().name);
	}

	private static final class Payload {
		private String name;

		public String getName() {
			return name;
		}
	}
}
