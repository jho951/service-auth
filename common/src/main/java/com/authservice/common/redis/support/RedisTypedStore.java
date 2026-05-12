package com.authservice.common.redis.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 데이터 접근을 추상화하여 타입 안정성(Type-safety)을 제공하는 지원 클래스입니다.
 * <p>
 * {@link RedisTemplate}을 직접 사용할 때 발생할 수 있는 타입 캐스팅 오류를 방지하고,
 * JSON 직렬화/역직렬화 과정을 캡슐화하여 서비스 레이어의 복잡도를 낮춥니다.
 * </p>
 *
 * @author jho951
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
public class RedisTypedStore {

	/** * 다양한 객체 타입을 지원하기 위해 Object 타입으로 구성된 RedisTemplate 입니다.
	 * {@code com.authservice.common.redis.config.RedisConfig}에서 설정된 빈을 주입받습니다.
	 */
	private final RedisTemplate<String, Object> redisTemplate;

	/** * Redis에서 읽어온 LinkedHashMap 등의 데이터를 실제 Target 클래스로 변환하기 위한 매퍼입니다.
	 */
	private final ObjectMapper objectMapper;

	/**
	 * 데이터를 Redis에 저장합니다.
	 * 실무적인 메모리 관리를 위해 만료 시간(TTL) 설정을 강제합니다.
	 *
	 * @param key   저장할 고유 키
	 * @param value 저장할 객체 (직렬화 가능한 객체)
	 * @param ttl   데이터 유지 시간 (Time To Live)
	 */
	public void save(String key, Object value, Duration ttl) {
		redisTemplate.opsForValue().set(key, value, ttl);
	}

	/**
	 * 특정 키가 Redis에 존재하는지 확인합니다.
	 *
	 * @param key 확인 대상 키
	 * @return 존재 여부
	 */
	public boolean hasKey(String key) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(key));
	}

	/**
	 * 특정 키에 해당하는 데이터를 삭제합니다.
	 *
	 * @param key 삭제 대상 키
	 */
	public void delete(String key) {
		redisTemplate.delete(key);
	}

	/**
	 * Redis에서 데이터를 조회하고 요청한 타입으로 변환하여 반환합니다.
	 * <p>
	 * <b>변환 로직:</b>
	 * <ol>
	 * <li>값이 존재하지 않으면 {@link Optional#empty()} 반환</li>
	 * <li>조회된 값이 요청 타입과 일치하면 즉시 캐스팅하여 반환</li>
	 * <li>타입이 일치하지 않을 경우(예: 복합 객체), ObjectMapper를 통해 타입 변환 후 반환</li>
	 * </ol>
	 * </p>
	 *
	 * @param <T>  반환받고자 하는 객체의 타입
	 * @param key  조회할 키
	 * @param type 반환 타입 클래스
	 * @return 데이터가 존재할 경우 해당 타입으로 변환된 {@link Optional}, 존재하지 않을 경우 빈 Optional
	 */
	public <T> Optional<T> find(String key, Class<T> type) {
		Object value = redisTemplate.opsForValue().get(key);
		if (value == null) return Optional.empty();
		if (type.isInstance(value)) return Optional.of(type.cast(value));

		return Optional.of(objectMapper.convertValue(value, type));
	}
}
