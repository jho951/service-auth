package com.authservice.app.security.platform.operational;

import io.github.jho951.platform.governance.api.GovernanceAuditSink;
import io.github.jho951.platform.governance.audit.LoggingAuditSink;
import io.github.jho951.platform.security.ratelimit.PlatformRateLimitDecision;
import io.github.jho951.platform.security.ratelimit.PlatformRateLimitKeyType;
import io.github.jho951.platform.security.ratelimit.PlatformRateLimitPort;
import io.github.jho951.platform.security.ratelimit.PlatformRateLimitRequest;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 운영 프로필에서만 활성화되는 platform 보안/거버넌스 운영 설정입니다.
 */
@Configuration
@Profile({"prod", "production", "live"})
public class AuthPlatformOperationalConfiguration {

    /**
     * 운영 환경에서 사용할 거버넌스 감사 싱크를 생성합니다.
     *
     * @return 로깅 기반 감사 싱크
     */
    @Bean
    public GovernanceAuditSink authGovernanceAuditSink() {
        return new LoggingAuditSink();
    }

    /**
     * Redis 기반 고정 윈도우 레이트 리미터를 생성합니다.
     *
     * @param redisTemplate 문자열 Redis 템플릿
     * @param keyPrefix 레이트 리밋 키 prefix
     * @return 플랫폼 레이트 리밋 포트
     */
    @Bean
    public PlatformRateLimitPort platformSecurityRateLimiter(
        StringRedisTemplate redisTemplate,
        @Value("${PLATFORM_SECURITY_RATE_LIMIT_REDIS_PREFIX:platform-security:rate-limit:auth-service:}")
        String keyPrefix
    ) {
        return new RedisFixedWindowPlatformRateLimitPort(redisTemplate, keyPrefix, Clock.systemUTC());
    }

    /** Redis에 카운터를 저장하는 고정 윈도우 레이트 리미터 구현입니다. */
    private static final class RedisFixedWindowPlatformRateLimitPort implements PlatformRateLimitPort {
        private final StringRedisTemplate redisTemplate;
        private final String keyPrefix;
        private final Clock clock;

        private RedisFixedWindowPlatformRateLimitPort(
            StringRedisTemplate redisTemplate,
            String keyPrefix,
            Clock clock
        ) {
            this.redisTemplate = redisTemplate;
            this.keyPrefix = keyPrefix == null ? "" : keyPrefix;
            this.clock = clock;
        }

        @Override
        public PlatformRateLimitDecision evaluate(PlatformRateLimitRequest request) {
            long windowSeconds = Math.max(1L, request.windowSeconds());
            long nowSeconds = clock.instant().getEpochSecond();
            long windowIndex = nowSeconds / windowSeconds;
            long windowEndSeconds = (windowIndex + 1L) * windowSeconds;
            String redisKey = keyPrefix
                + keyTypeSegment(request.keyType())
                + ":"
                + request.key()
                + ":"
                + windowIndex;

            Long current = redisTemplate.opsForValue().increment(redisKey, request.permits());
            if (current == null) {
                return PlatformRateLimitDecision.deny(request.key(), "rate limit backend unavailable");
            }
            if (current == request.permits()) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds + 1L));
            }

            if (current <= request.limit()) {
                return PlatformRateLimitDecision.allow(request.key(), "within rate limit");
            }

            long retryAfterSeconds = Math.max(0L, windowEndSeconds - nowSeconds);
            return PlatformRateLimitDecision.deny(
                request.key(),
                "rate limit exceeded for " + request.key() + "; retry_after_seconds=" + retryAfterSeconds
            );
        }

        private String keyTypeSegment(PlatformRateLimitKeyType keyType) {
            return keyType == PlatformRateLimitKeyType.USER ? "user" : "ip";
        }
    }
}
