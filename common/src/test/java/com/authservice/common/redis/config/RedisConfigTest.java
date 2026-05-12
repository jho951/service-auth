package com.authservice.common.redis.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

class RedisConfigTest {

	private final RedisConfig config = new RedisConfig();

	@Test
	void redisTemplateUsesExpectedSerializers() {
		RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

		RedisTemplate<String, Object> template = config.redisTemplate(connectionFactory);

		assertEquals(connectionFactory, template.getConnectionFactory());
		assertInstanceOf(StringRedisSerializer.class, template.getKeySerializer());
		assertInstanceOf(StringRedisSerializer.class, template.getHashKeySerializer());
		assertInstanceOf(GenericJackson2JsonRedisSerializer.class, template.getValueSerializer());
		assertInstanceOf(GenericJackson2JsonRedisSerializer.class, template.getHashValueSerializer());
	}

	@Test
	void redisTemplateValueSerializerPreservesTypeInformationAndJavaTime() {
		RedisTemplate<String, Object> template = config.redisTemplate(mock(RedisConnectionFactory.class));
		Payload payload = new Payload();
		payload.name = "alice";
		payload.createdAt = LocalDateTime.of(2026, 4, 27, 9, 30);
		@SuppressWarnings("unchecked")
		RedisSerializer<Object> serializer = (RedisSerializer<Object>) template.getValueSerializer();

		byte[] serialized = serializer.serialize(payload);
		Object deserialized = serializer.deserialize(serialized);

		assertNotNull(serialized);
		assertTrue(serialized.length > 0);
		assertInstanceOf(Payload.class, deserialized);
		Payload restored = (Payload) deserialized;
		assertEquals("alice", restored.name);
		assertEquals(LocalDateTime.of(2026, 4, 27, 9, 30), restored.createdAt);
	}

	private static class Payload {
		private String name;
		private LocalDateTime createdAt;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public LocalDateTime getCreatedAt() {
			return createdAt;
		}

		public void setCreatedAt(LocalDateTime createdAt) {
			this.createdAt = createdAt;
		}
	}
}
