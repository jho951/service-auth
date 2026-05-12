package com.authservice.common.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

/**
 * Redis 인프라 설정을 담당하는 구성 클래스입니다.
 * <p>
 * 애플리케이션에서 사용하는 {@link RedisTemplate}의 직렬화 방식을 정의하며,
 * 특히 객체를 JSON 형태로 저장할 때의 데이터 일관성과 타입 안정성을 보장하기 위한 설정을 포함합니다.
 * </p>
 */
@Configuration
public class RedisConfig {

	/**
	 * Redis 작업을 수행하기 위한 {@link RedisTemplate} 빈을 생성합니다.
	 * <p>
	 * <b>설정 상세:</b>
	 * <ul>
	 * <li><b>Key Serializer:</b> Redis 키를 가독성이 좋은 평문 문자열로 저장하기 위해 {@link StringRedisSerializer}를 사용합니다.</li>
	 * <li><b>Value Serializer:</b> 객체를 JSON으로 직렬화하여 저장하기 위해 {@link GenericJackson2JsonRedisSerializer}를 사용합니다.</li>
	 * <li><b>ObjectMapper 설정:</b>
	 * <ul>
	 * <li>날짜 처리: {@link JavaTimeModule}을 등록하여 LocalDateTime 등의 ISO-8601 포맷을 지원합니다.</li>
	 * <li>타입 보존: 역직렬화 시 LinkedHashMap 전환 문제를 방지하기 위해 클래스 타입 정보를 JSON에 포함합니다.</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * </p>
	 *
	 * @param redisConnectionFactory Redis 서버와의 연결을 관리하는 팩토리 객체
	 * @return 설정이 완료된 RedisTemplate 인스턴스
	 */
	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(redisConnectionFactory);

		// JSON 직렬화를 위한 ObjectMapper 세부 설정
		ObjectMapper mapper = new ObjectMapper();

		// 1. Java 8 날짜/시간 모듈 활성화
		mapper.registerModule(new JavaTimeModule());

		// 2. 날짜를 숫자 배열이 아닌 표준 ISO-8601 문자열로 저장하도록 설정
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		// 3. 다형성 처리를 위한 기본 타입 정보 포함 설정
		// 역직렬화 시 구체적인 객체 타입을 찾아내기 위해 '@class' 필드를 JSON에 추가함
		mapper.activateDefaultTyping(
			LaissezFaireSubTypeValidator.instance,
			ObjectMapper.DefaultTyping.NON_FINAL,
			JsonTypeInfo.As.PROPERTY
		);

		GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(mapper);

		// Key-Value 직렬화 설정
		template.setKeySerializer(new StringRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());

		template.setValueSerializer(jsonSerializer);
		template.setHashValueSerializer(jsonSerializer);

		template.afterPropertiesSet();
		return template;
	}
}