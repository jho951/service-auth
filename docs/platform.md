# Platform 사용 기준

Auth-server는 `platform-security`와 `platform-governance`를 공통 경계로만 사용합니다.
MSA 인증 표준, 공통 identity header, 외부 credential 허용 정책, 이벤트 계약은 contract 레포를 기준으로 합니다.

기준 버전:

| Platform | Version |
| --- | --- |
| `platform-security` | `1.0.4` |
| `platform-governance` | `1.1.0` |

## Security

Auth-server는 token/session issuer가 주 역할이므로 `platform-security-issuer-starter`만 사용합니다.
일부 internal endpoint는 별도 starter를 추가하지 않고 `platform.security.boundary.internal-paths`와 internal CIDR 정책으로 분류합니다.

```gradle
implementation platform(libs.platform.security.bom)
implementation libs.platform.security.issuer.starter
testImplementation libs.platform.security.test.support
```

역할별 starter는 endpoint 종류가 아니라 서비스의 주 역할(primary role)을 기준으로 하나만 선택합니다.

| 서비스 역할 | starter |
| --- | --- |
| Gateway/edge | `platform-security-edge-starter` |
| token/session issuer | `platform-security-issuer-starter` |
| 일반 resource API | `platform-security-resource-server-starter` |
| 내부 호출 전용 서비스 | `platform-security-internal-service-starter` |

Auth-server가 소유합니다.

- provider OAuth2 authorization/callback 처리
- user-service 사용자 조회/생성/연결
- 계정 상태와 도메인 정책 확인
- access/refresh token 발급
- SSO session 저장
- 서비스별 redirect와 cookie 정책

`platform-security`가 소유합니다.

- 요청 credential을 공통 security request로 변환
- 인증 결과를 `SecurityContext`로 정규화
- downstream identity propagation 경계 제공
- audit/security hook 연결점 제공
- servlet/webflux adapter 제공

Security 구현 규칙:

- 도메인, 컨트롤러, 서비스 코드는 `com.auth.*` 1계층 구현 타입을 직접 import하지 않습니다.
- `PlatformSecurityAuthConfig`에서만 `TokenService`, `SessionStore`, `SessionPrincipalMapper` adapter를 제공합니다.
- `SecurityContextResolver`는 `PlatformSecurityContextResolvers.hybrid(...)`로 생성합니다.
- `SecurityConfig`는 auto-config가 만든 `securityServletFilter`를 Spring Security filter chain에 연결합니다.
- access token 검증은 `AuthJwtTokenService`, SSO session 조회는 `SsoSessionStore`가 담당합니다.
- cookie 이름은 `platform.security`가 아니라 Auth-server의 auth/SSO 설정에서 관리합니다.
- OIDC verifier는 Auth-server가 외부 OIDC ID token을 API credential로 직접 받을 때만 추가합니다.
- `/actuator/health/**`만 public으로 두고 metrics/prometheus 등은 인증 또는 네트워크 정책으로 보호합니다.
- public auth endpoint는 route-specific rate limit을 둡니다.
- admin/internal CIDR은 파일 수정이 아니라 `PLATFORM_SECURITY_*_ALLOW_CIDRS` 환경변수로 조정합니다.
- 새 credential 방식을 허용할 때는 contract 레포의 인증 정책을 먼저 갱신합니다.

Security 운영 환경변수:

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `AUTH_JWT_SECRET` | 없음 | JWT 서명 secret. 운영에서는 반드시 주입합니다. |
| `PLATFORM_SECURITY_ADMIN_ALLOW_CIDRS` | `10.0.0.0/8` | admin boundary 허용 CIDR 목록 |
| `PLATFORM_SECURITY_INTERNAL_ALLOW_CIDRS` | `172.16.0.0/12` | internal boundary 허용 CIDR 목록 |
| `PLATFORM_SECURITY_RATE_LIMIT_AUTH_LOGIN_REQUESTS` | `10` | 로그인/OAuth 시작 route 요청 수 |
| `PLATFORM_SECURITY_RATE_LIMIT_AUTH_LOGIN_WINDOW_SECONDS` | `60` | 로그인/OAuth 시작 route window |
| `PLATFORM_SECURITY_RATE_LIMIT_AUTH_REFRESH_REQUESTS` | `20` | refresh/exchange route 요청 수 |
| `PLATFORM_SECURITY_RATE_LIMIT_AUTH_REFRESH_WINDOW_SECONDS` | `60` | refresh/exchange route window |

## Governance

Auth-server는 `platform-governance`를 감사 기록과 governance policy 경계로만 사용합니다.
도메인 판단과 이벤트 생성 시점은 Auth-server가 소유합니다.

```gradle
implementation platform(libs.platform.governance.bom)
implementation libs.platform.governance.starter
```

Auth-server 도메인 코드는 governance 1계층 구현 타입을 직접 소비하지 않습니다.

- `com.auditlog.*`
- `com.policyconfig.*`
- `com.pluginpolicyengine.*`

직접 사용하는 타입은 2계층 경계 타입으로 제한합니다.

| 용도 | 타입 |
| --- | --- |
| 감사 이벤트 기록 | `io.github.jho951.platform.governance.api.AuditLogRecorder` |
| 감사 이벤트 envelope | `io.github.jho951.platform.governance.api.AuditEntry` |

Governance 구현 규칙:

- `AuthAuditLogService`만 `AuditLogRecorder`를 주입받습니다.
- 로그인, 로그아웃, internal account 변경 시점은 Auth-server 도메인 코드에서 명시합니다.
- `AuthAuditLogService`는 도메인 이벤트를 `AuditEntry`로 변환하는 adapter 역할만 합니다.
- `platform.governance.*` 설정은 `application.yml`과 환경변수에서 관리합니다.
- business decision을 governance 모듈에 숨기지 않습니다.

## Governance 설정

```yaml
platform:
  governance:
    enabled: true
    audit:
      enabled: true
    plugin-policy-engine:
      store: MEMORY
      cache-ttl-millis: 3000
    engine:
      strict: false
    violation:
      action: DENY
      handler-failure-fatal: false
    operational:
      fail-fast-enabled: true
      production-profiles: prod
      require-audit-sink-in-production: true
      require-policy-config-in-enforcing-mode: true
      require-fatal-handler-failures-in-production: true
```

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `PLATFORM_GOVERNANCE_ENABLED` | `true` | platform-governance auto-configuration 활성화 |
| `PLATFORM_GOVERNANCE_AUDIT_ENABLED` | `true` | audit recorder 활성화 |
| `PLATFORM_GOVERNANCE_POLICY_ENGINE_STORE` | `MEMORY` | policy engine store |
| `PLATFORM_GOVERNANCE_POLICY_ENGINE_FILE_PATH` | 빈 값 | file store path |
| `PLATFORM_GOVERNANCE_POLICY_ENGINE_CACHE_TTL_MILLIS` | `3000` | policy engine cache TTL |
| `PLATFORM_GOVERNANCE_ENGINE_STRICT` | `false` | strict evaluation |
| `PLATFORM_GOVERNANCE_VIOLATION_ACTION` | `DENY` | policy violation 처리 방식 |
| `PLATFORM_GOVERNANCE_VIOLATION_HANDLER_FAILURE_FATAL` | `false` | violation handler 실패를 fatal로 볼지 여부 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_FAIL_FAST_ENABLED` | `true` | 운영 정책 위반 시 기동 실패 여부 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_PRODUCTION_PROFILES` | `prod` | 운영 profile 목록 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_REQUIRE_AUDIT_SINK_IN_PRODUCTION` | `true` | 운영에서 audit sink 요구 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_REQUIRE_POLICY_CONFIG_IN_ENFORCING_MODE` | `true` | enforcing mode에서 policy config 요구 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_REQUIRE_FATAL_HANDLER_FAILURES_IN_PRODUCTION` | `true` | 운영에서 handler failure fatal 요구 |

## 감사 이벤트

| Event | Category | 주요 attributes |
| --- | --- | --- |
| `AUTH_LOGIN_PASSWORD` | `auth` | `eventType`, `result`, `actorId`, `channel` |
| `AUTH_LOGIN_SSO` | `auth` | `eventType`, `result`, `actorId`, `provider` |
| `AUTH_LOGOUT` | `auth` | `eventType`, `result`, `actorId`, `channel` |
| `AUTH_INTERNAL_ACCOUNT_CREATE` | `auth` | `eventType`, `actorType`, `resourceId`, `loginId` |
| `AUTH_INTERNAL_ACCOUNT_DELETE` | `auth` | `eventType`, `actorType`, `resourceId` |
