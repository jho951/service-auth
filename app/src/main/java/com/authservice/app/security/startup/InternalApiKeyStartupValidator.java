package com.authservice.app.security.startup;

import com.authservice.app.domain.auth.userdirectory.config.InternalApiProperties;
import java.util.Arrays;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 운영 환경에서 내부 API 키가 안전한 값으로 설정되었는지 검증하는 시작 시점 검사기입니다.
 */
@Component
public class InternalApiKeyStartupValidator implements ApplicationRunner {

	private static final String LOCAL_INTERNAL_API_KEY = "local-internal-api-key";

	private final Environment environment;
	private final InternalApiProperties internalApiProperties;

	/**
	 * 현재 실행 환경과 내부 API 키 설정 소스를 주입합니다.
	 *
	 * @param environment 활성 프로필 조회용 환경
	 * @param internalApiProperties 내부 API 인증 설정
	 */
	public InternalApiKeyStartupValidator(Environment environment, InternalApiProperties internalApiProperties) {
		this.environment = environment;
		this.internalApiProperties = internalApiProperties;
	}

	/**
	 * 현재 실행 프로필과 내부 API 키 구성을 검증합니다.
	 *
	 * @param args 애플리케이션 시작 인자
	 */
	@Override
	public void run(ApplicationArguments args) {
		if (!isProdProfile()) {
			return;
		}
		String key = internalApiProperties.getKey();
		if (key == null || key.isBlank() || LOCAL_INTERNAL_API_KEY.equals(key)) {
			throw new IllegalStateException("INTERNAL_API_KEY must be explicitly configured in prod");
		}
	}

	private boolean isProdProfile() {
		return Arrays.asList(environment.getActiveProfiles()).contains("prod");
	}
}
