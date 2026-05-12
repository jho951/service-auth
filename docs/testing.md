# Testing

## Strategy

현재 테스트는 네 범위로 나뉩니다.

- controller/flow 테스트
- security/config alignment 테스트
- service/unit 테스트
- Redis/application integration 테스트

## What The Tests Verify

| Area | Representative Tests | Why |
| --- | --- | --- |
| Password auth flow | `AuthGatewayControllerFlowTest` | login, refresh 흐름 회귀 방지 |
| SSO flow | `SsoControllerFlowTest`, `SsoOAuthFlowServiceIpGuardTest` | redirect, ticket, admin IP guard |
| Internal validation | `InternalSessionControllerFlowTest` | Gateway용 session validation |
| Redis behavior | `RedisIntegrationTests`, `AuthUserFinderCacheTest` | cache/store 연결과 fallback |
| JWT/security | `AuthJwtTokenServiceTest`, `CookieCsrfOriginGuardFilterTest` | token 발급, cookie origin guard |
| Platform config | `PlatformSecurityAuthConfigTest`, `PlatformSecurityBoundaryConfigAlignmentTest`, `AuthPlatformGovernanceConfigurationTest` | platform-security/governance 연결점 검증 |
| Audit | `AuthAuditLogServiceTest`, `GovernanceSecurityAuditPublisherTest` | 감사 이벤트 shape 검증 |

## Commands

전체 테스트:

```bash
./gradlew test
```

빠른 컴파일 확인:

```bash
./gradlew :common:compileJava :app:compileJava
```

애플리케이션 빌드:

```bash
./gradlew :app:bootJar
```

## Coverage Note

현재 레포에는 전용 JaCoCo 설정이 없습니다. 따라서 공식 coverage threshold를 빌드가 강제하지는 않습니다. 대신 보안 경계, 인증 흐름, platform 정렬 테스트를 넓게 유지하는 쪽에 무게를 둡니다.

## Test Placement Rules

- production package를 따라 단위 테스트를 배치합니다.
- 여러 패키지를 묶는 흐름 검증은 `com.authservice.app` 상위 테스트에 둡니다.
- Redis/애플리케이션 통합 검증은 이름과 목적이 드러나게 유지합니다.

## Minimum Verification For Changes

- 인증 로직 변경: 관련 flow test + service test
- security 설정 변경: config alignment/security route test
- Redis/session 변경: Redis 관련 테스트
- 계약 변경: OpenAPI sync와 함께 `./gradlew test`

## Working Rules

- 구현보다 contract를 먼저 변경합니다.
- 새 모듈/문서 파일 추가보다 기존 문서와 경계에 흡수하는 쪽을 우선합니다.
- DB 변경이면 `db/schema.sql`을 먼저 갱신합니다.
- public/internal API 변경은 OpenAPI와 테스트를 같이 갱신합니다.
