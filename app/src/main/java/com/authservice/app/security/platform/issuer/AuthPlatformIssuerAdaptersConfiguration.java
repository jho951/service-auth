package com.authservice.app.security.platform.issuer;

import com.authservice.app.domain.auth.config.AuthHttpProperties;
import com.authservice.app.domain.auth.model.AuthPrincipal;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoSessionPayload;
import com.authservice.app.domain.auth.sso.service.SsoSessionStore;
import com.authservice.app.security.jwt.AuthJwtTokenService;
import io.github.jho951.platform.security.auth.PlatformAuthenticatedPrincipal;
import io.github.jho951.platform.security.auth.PlatformIssuedToken;
import io.github.jho951.platform.security.auth.PlatformSessionIssuerPort;
import io.github.jho951.platform.security.auth.PlatformSessionSupport;
import io.github.jho951.platform.security.auth.PlatformSessionSupportFactory;
import io.github.jho951.platform.security.auth.PlatformSessionView;
import io.github.jho951.platform.security.auth.PlatformTokenIssuerPort;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * auth-service 고유 토큰/세션 모델을 platform-security 발급 포트에 연결하는 어댑터 설정입니다.
 */
@Configuration
public class AuthPlatformIssuerAdaptersConfiguration {

    /**
     * platform-security의 토큰 발급 포트를 auth-service JWT 발급기로 연결합니다.
     *
     * @param tokenService auth-service JWT 서비스
     * @return 플랫폼 토큰 발급 포트
     */
    @Bean
    public PlatformTokenIssuerPort platformTokenIssuerPort(AuthJwtTokenService tokenService) {
        return command -> {
            AuthPrincipal principal = toAuthPrincipal(command.principal());
            return new PlatformIssuedToken(
                tokenService.issueAccessToken(principal),
                tokenService.issueRefreshToken(principal)
            );
        };
    }

    /**
     * platform-security의 세션 발급 포트를 auth-service SSO 세션 저장소에 연결합니다.
     *
     * @param sessionStore SSO 세션 저장소
     * @param properties 인증 HTTP 설정
     * @return 플랫폼 세션 발급 포트
     */
    @Bean
    public PlatformSessionIssuerPort platformSessionIssuerPort(
        SsoSessionStore sessionStore,
        AuthHttpProperties properties
    ) {
        Duration sessionTtl = Duration.ofSeconds(Math.max(1L, properties.getJwt().getRefreshSeconds()));
        return command -> {
            Instant expiresAt = Instant.now().plus(sessionTtl);
            PlatformAuthenticatedPrincipal principal = command.principal();
            String sessionId = UUID.randomUUID().toString();
            sessionStore.saveSession(sessionId, toSessionPayload(principal, expiresAt), expiresAt);
            return new PlatformSessionView(sessionId, principal);
        };
    }

    /**
     * platform-security가 사용할 세션 지원 팩토리를 생성합니다.
     *
     * @param tokenService auth-service JWT 서비스
     * @param sessionStore SSO 세션 저장소
     * @return 플랫폼 세션 지원 팩토리
     */
    @Bean
    public PlatformSessionSupportFactory platformSessionSupportFactory(
        AuthJwtTokenService tokenService,
        SsoSessionStore sessionStore
    ) {
        return () -> new ServiceOwnedPlatformSessionSupport(tokenService, sessionStore);
    }

    private static AuthPrincipal toAuthPrincipal(PlatformAuthenticatedPrincipal principal) {
        return new AuthPrincipal(
            principal.userId(),
            principal.authorities().stream().toList(),
            principal.attributes()
        );
    }

    private static PlatformAuthenticatedPrincipal toPlatformPrincipal(AuthPrincipal principal) {
        return new PlatformAuthenticatedPrincipal(
            principal.userId(),
            new java.util.LinkedHashSet<>(principal.roles()),
            principal.attributes()
        );
    }

    private static PlatformAuthenticatedPrincipal toPlatformPrincipal(SsoSessionPayload payload) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        putIfPresent(attributes, "email", payload.getEmail());
        putIfPresent(attributes, "name", payload.getName());
        putIfPresent(attributes, "avatarUrl", payload.getAvatarUrl());
        putIfPresent(attributes, "status", payload.getStatus());
        return new PlatformAuthenticatedPrincipal(
            payload.getUserId(),
            new java.util.LinkedHashSet<>(payload.getRoles() == null ? java.util.List.of() : payload.getRoles()),
            attributes
        );
    }

    private static SsoSessionPayload toSessionPayload(PlatformAuthenticatedPrincipal principal, Instant expiresAt) {
        return new SsoSessionPayload(
            principal.userId(),
            stringAttribute(principal.attributes(), "email"),
            stringAttribute(principal.attributes(), "name"),
            stringAttribute(principal.attributes(), "avatarUrl"),
            principal.authorities().stream().toList(),
            stringAttribute(principal.attributes(), "status"),
            expiresAt
        );
    }

    private static String stringAttribute(Map<String, Object> attributes, String key) {
        if (attributes == null) {
            return null;
        }
        Object value = attributes.get(key);
        return value instanceof String string && !string.isBlank() ? string : null;
    }

    private static void putIfPresent(Map<String, Object> attributes, String key, String value) {
        if (value != null && !value.isBlank()) {
            attributes.put(key, value);
        }
    }

    private static final class ServiceOwnedPlatformSessionSupport implements PlatformSessionSupport {
        private final AuthJwtTokenService tokenService;
        private final SsoSessionStore sessionStore;

        private ServiceOwnedPlatformSessionSupport(
            AuthJwtTokenService tokenService,
            SsoSessionStore sessionStore
        ) {
            this.tokenService = tokenService;
            this.sessionStore = sessionStore;
        }

        @Override
        public Optional<PlatformAuthenticatedPrincipal> authenticate(String accessToken, String sessionId) {
            Optional<PlatformAuthenticatedPrincipal> tokenPrincipal = lookupAccessToken(accessToken);
            if (tokenPrincipal.isPresent()) {
                return tokenPrincipal;
            }
            return lookupSession(sessionId);
        }

        private Optional<PlatformAuthenticatedPrincipal> lookupAccessToken(String accessToken) {
            if (accessToken == null || accessToken.isBlank()) {
                return Optional.empty();
            }
            try {
                return Optional.of(toPlatformPrincipal(tokenService.verifyAccessToken(accessToken)));
            } catch (RuntimeException ex) {
                return Optional.empty();
            }
        }

        private Optional<PlatformAuthenticatedPrincipal> lookupSession(String sessionId) {
            if (sessionId == null || sessionId.isBlank()) {
                return Optional.empty();
            }
            return sessionStore.findSession(sessionId).map(AuthPlatformIssuerAdaptersConfiguration::toPlatformPrincipal);
        }
    }
}
