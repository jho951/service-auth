package com.authservice.app.security.platform.governance;

import io.github.jho951.platform.governance.api.PolicyConfigSource;
import io.github.jho951.platform.governance.config.MapPolicyConfigSource;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;

/**
 * platform-governance 정책 설정 소스를 auth-service 환경설정으로부터 구성합니다.
 */
@Configuration
public class AuthPlatformGovernanceConfiguration {
    private static final String POLICY_CONFIG_VALUES_PREFIX = "platform.governance.policy-config.values";

    /**
     * 환경 설정에 선언된 정책 값을 platform-governance용 {@link PolicyConfigSource}로 노출합니다.
     *
     * @param environment 현재 실행 환경
     * @return 정책 설정 소스
     */
    @Bean
    public PolicyConfigSource authGovernancePolicyConfigSource(
        Environment environment
    ) {
        Map<String, String> configuredValues = Binder
            .get(environment)
            .bind(POLICY_CONFIG_VALUES_PREFIX, Bindable.mapOf(String.class, String.class))
            .orElseGet(Map::of);
        return new MapPolicyConfigSource(sanitizedPolicyConfigValues(configuredValues));
    }

    /**
     * 정책 설정 맵에서 빈 키와 null 값을 제거합니다.
     *
     * @param configuredValues 원본 정책 설정 값
     * @return 정리된 정책 설정 맵
     */
    static Map<String, String> sanitizedPolicyConfigValues(Map<String, String> configuredValues) {
        if (configuredValues == null || configuredValues.isEmpty()) {
            return Map.of();
        }

        Map<String, String> sanitized = new LinkedHashMap<>();
        configuredValues.forEach((key, value) -> {
            if (key == null) {
                return;
            }
            String trimmedKey = key.trim();
            if (trimmedKey.isEmpty() || value == null) {
                return;
            }
            sanitized.put(trimmedKey, value);
        });
        return sanitized;
    }
}
