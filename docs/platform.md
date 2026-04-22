# Platform 사용 기준

auth-service는 `platform-security`, `platform-governance`, `platform-integrations`를 공통 경계로 사용합니다.
MSA 인증 표준, 공통 identity header, 외부 credential 허용 정책, 이벤트 계약은 contract 레포를 기준으로 합니다.

버전:

| Platform | Version |
| --- | --- |
| `platform-security` | `2.0.5` |
| `platform-governance` | `2.0.2` |
| `platform-integrations` | `1.0.3` |


## Security

auth-service는 token/session issuer가 주 역할이므로 `platform-security-starter`와 `ISSUER` preset을 사용합니다.
일부 internal endpoint는 별도 starter를 추가하지 않고 `platform.security.boundary.internal-paths`와 internal CIDR 정책으로 분류합니다.

```gradle
implementation platform(libs.platform.security.bom)
implementation libs.platform.security.starter
```

역할 preset은 endpoint 종류가 아니라 서비스의 주 역할(primary role)을 기준으로 하나만 선택합니다.

| 서비스 역할 | preset |
| --- | --- |
| Gateway/edge | `EDGE` |
| token/session issuer | `ISSUER` |
| 일반 resource API | `API_SERVER` |
| 내부 호출 전용 서비스 | `INTERNAL_SERVICE` |

auth-service가 소유합니다.

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

- 도메인, 컨트롤러, 서비스 코드는 `com.auth.*`, `com.ipguard.*` 1계층 구현 타입을 직접 import하지 않습니다.
- auth-service의 `platform-security` 직접 의존은 `platform-security-bom`과 `platform-security-starter`로 제한합니다.
- security audit event를 governance audit-log로 연결할 때만 `platform-integrations`의 `platform-security-governance-bridge`를 사용합니다.
- `PlatformSecurityAuthConfig`는 starter가 소비하는 `SecurityContextResolver`를 제공합니다.
- `SecurityConfig`는 auto-config가 만든 `securityServletFilter`를 Spring Security filter chain에 연결합니다.
- access token 검증은 `AuthJwtTokenService`, SSO session 조회는 `SsoSessionStore`가 담당합니다.
- cookie 이름은 `platform.security`가 아니라 auth-service의 auth/SSO 설정에서 관리합니다.
- OIDC verifier는 auth-service가 외부 OIDC ID token을 API credential로 직접 받을 때만 추가합니다.
- `/actuator/health/**`만 public으로 두고 metrics/prometheus 등은 인증 또는 네트워크 정책으로 보호합니다.
- public auth endpoint는 route-specific rate limit을 둡니다.
- admin/internal CIDR은 파일 수정이 아니라 `PLATFORM_SECURITY_*_ALLOW_CIDRS` 환경변수로 조정합니다.
- 새 credential 방식을 허용할 때는 contract 레포의 인증 정책을 먼저 갱신합니다.

Route 분류:

| Route | 최종 분류 | Spring Security | platform-security | 앱 내부 검증 |
| --- | --- | --- | --- | --- |
| `/auth/me` | `PROTECTED` | `authenticated` | protected boundary | 없음 |
| `/auth/session` | `PROTECTED` | `authenticated` | protected boundary | 없음 |
| `/auth/internal/session/validate` | `INTERNAL` | internal pass-through | internal boundary | internal secret 또는 internal token |
| `/internal/**` | `INTERNAL` | internal pass-through | internal boundary | internal secret 또는 internal token |

Internal route는 public endpoint가 아닙니다. 현재는 `platform.security.auth.internal-token-enabled=false`이므로 Spring Security에서 internal 인증 주체를 만들지 않고 pass-through합니다. 대신 network/IP boundary는 `platform-security`, caller secret 검증은 `InternalEndpointAccessFilter`와 `InternalApiProperties`가 담당하며 둘 중 하나라도 실패하면 요청을 거부합니다.

`INTERNAL_API_KEY`는 운영에서 반드시 명시적으로 주입합니다. 루트 설정은 기본값을 두지 않고, local 개발 기본값은 dev profile에만 둡니다. prod profile에서 key가 비어 있거나 `local-internal-api-key`이면 애플리케이션은 기동하지 않습니다.

Internal token 도입 목표:

- `InternalTokenClaimsValidator`를 구현합니다.
- `/auth/internal/**`, `/internal/**` 전용 Spring Security chain을 둡니다.
- internal auth filter가 `ROLE_INTERNAL` 또는 동등 authority를 설정합니다.
- internal route는 `hasAuthority("ROLE_INTERNAL")`로 전환합니다.

Admin guard 구분:

| 설정 | 역할 |
| --- | --- |
| `sso.frontend.admin.ip-guard` | SSO admin page, redirect, frontend entry 보호 |
| `platform.security.ip-guard.admin` | `/admin/**` 같은 service route boundary 보호 |

Cookie CSRF 방어:

- Spring Security CSRF token은 `/auth/refresh`, `/auth/logout`에서 사용하지 않습니다.
- 대신 cookie 인증이 동반된 `POST /auth/refresh`, `POST /auth/logout`은 `CookieCsrfOriginGuardFilter`가 `Origin` 또는 `Referer`를 검증합니다.
- 허용 origin은 `sso.frontend.*.origin` 설정에서 계산한 목록과 일치해야 합니다.
- bearer-only client처럼 auth cookie가 없는 요청은 이 origin guard 대상이 아닙니다.

Security 운영 환경변수:

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `AUTH_JWT_SECRET` | 없음 | JWT 서명 secret. 운영에서는 반드시 주입합니다. |
| `INTERNAL_API_KEY` | dev only | internal route caller proof. prod에서는 명시 주입 필수 |
| `PLATFORM_SECURITY_INTERNAL_TOKEN_ENABLED` | `false` | platform internal token capability 활성화. 켜려면 `InternalTokenClaimsValidator` 구현이 필요합니다. |
| `PLATFORM_SECURITY_ADMIN_ALLOW_CIDRS` | `10.0.0.0/8` | admin boundary 허용 CIDR 목록 |
| `PLATFORM_SECURITY_INTERNAL_ALLOW_CIDRS` | `172.16.0.0/12` | internal boundary 허용 CIDR 목록 |
| `PLATFORM_SECURITY_RATE_LIMIT_AUTH_LOGIN_REQUESTS` | `10` | 로그인/OAuth 시작 route 요청 수 |
| `PLATFORM_SECURITY_RATE_LIMIT_AUTH_LOGIN_WINDOW_SECONDS` | `60` | 로그인/OAuth 시작 route window |
| `PLATFORM_SECURITY_RATE_LIMIT_AUTH_REFRESH_REQUESTS` | `20` | refresh/exchange route 요청 수 |
| `PLATFORM_SECURITY_RATE_LIMIT_AUTH_REFRESH_WINDOW_SECONDS` | `60` | refresh/exchange route window |

## Governance

auth-service는 `platform-governance`를 감사 기록과 governance policy 경계로만 사용합니다.
도메인 판단과 이벤트 생성 시점은 auth-service가 소유합니다.

```gradle
implementation platform(libs.platform.governance.bom)
implementation libs.platform.governance.starter
implementation libs.platform.security.governance.bridge
```

auth-service 도메인 코드는 governance 1계층 구현 타입을 직접 소비하지 않습니다.

- `com.auditlog.*`
- `com.policyconfig.*`
- `com.pluginpolicyengine.*`


| 용도 | 타입 |
| --- | --- |
| 감사 이벤트 기록 | `io.github.jho951.platform.governance.api.AuditLogRecorder` |
| 감사 이벤트 envelope | `io.github.jho951.platform.governance.api.AuditEntry` |

Governance 구현 규칙:

- `AuthAuditLogService`만 `AuditLogRecorder`를 주입받습니다.
- 로그인, 로그아웃, internal account 변경 시점은 auth-service 도메인 코드에서 명시합니다.
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
      service-name: auth-service
      environment: prod
      failure-policy: FAIL_CLOSED
      identity:
        enabled: true
        validation-enabled: true
        fail-on-validation-error: true
    plugin-policy-engine:
      store: MEMORY
      cache-ttl-millis: 3000
    engine:
      strict: true
    violation:
      action: DENY
      handler-failure-fatal: true
    operational:
      fail-fast-enabled: true
      production-profiles: prod
      allow-audit-disabled-in-production: false
      allow-non-strict-engine-in-production: false
      allow-permissive-violation-action-in-production: false
      require-audit-sink-in-production: true
      require-audit-context-resolver-in-production: true
      require-audit-service-identity-in-production: true
      require-identity-audit-validation-in-production: true
      require-policy-config-in-enforcing-mode: true
      require-fatal-handler-failures-in-production: true
      allow-ignore-audit-failure-policy-in-production: false
```

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `PLATFORM_GOVERNANCE_ENABLED` | `true` | platform-governance auto-configuration 활성화 |
| `PLATFORM_GOVERNANCE_AUDIT_ENABLED` | `true` | audit recorder 활성화 |
| `PLATFORM_GOVERNANCE_AUDIT_SERVICE_NAME` | `auth-service` | 감사 service name |
| `PLATFORM_GOVERNANCE_AUDIT_ENVIRONMENT` | active profile | 감사 environment |
| `PLATFORM_GOVERNANCE_AUDIT_FAILURE_POLICY` | `FAIL_CLOSED` | audit failure policy |
| `PLATFORM_GOVERNANCE_AUDIT_IDENTITY_ENABLED` | `true` | identity audit 활성화 |
| `PLATFORM_GOVERNANCE_AUDIT_IDENTITY_VALIDATION_ENABLED` | `true` | identity audit validation 활성화 |
| `PLATFORM_GOVERNANCE_AUDIT_IDENTITY_FAIL_ON_VALIDATION_ERROR` | `true` | identity audit validation 실패 처리 |
| `PLATFORM_GOVERNANCE_POLICY_ENGINE_STORE` | `MEMORY` | policy engine store |
| `PLATFORM_GOVERNANCE_POLICY_ENGINE_FILE_PATH` | 빈 값 | file store path |
| `PLATFORM_GOVERNANCE_POLICY_ENGINE_CACHE_TTL_MILLIS` | `3000` | policy engine cache TTL |
| `PLATFORM_GOVERNANCE_ENGINE_STRICT` | `true` | strict evaluation |
| `PLATFORM_GOVERNANCE_VIOLATION_ACTION` | `DENY` | policy violation 처리 방식 |
| `PLATFORM_GOVERNANCE_VIOLATION_HANDLER_FAILURE_FATAL` | `true` | violation handler 실패를 fatal로 볼지 여부 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_FAIL_FAST_ENABLED` | `true` | 운영 정책 위반 시 기동 실패 여부 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_PRODUCTION_PROFILES` | `prod` | 운영 profile 목록 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_ALLOW_AUDIT_DISABLED_IN_PRODUCTION` | `false` | 운영에서 audit 비활성 허용 여부 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_ALLOW_NON_STRICT_ENGINE_IN_PRODUCTION` | `false` | 운영에서 non-strict engine 허용 여부 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_ALLOW_PERMISSIVE_VIOLATION_ACTION_IN_PRODUCTION` | `false` | 운영에서 permissive violation action 허용 여부 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_REQUIRE_AUDIT_SINK_IN_PRODUCTION` | `true` | 운영에서 audit sink 요구 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_REQUIRE_AUDIT_CONTEXT_RESOLVER_IN_PRODUCTION` | `true` | 운영에서 audit context resolver 요구 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_REQUIRE_AUDIT_SERVICE_IDENTITY_IN_PRODUCTION` | `true` | 운영에서 감사 service identity 요구 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_REQUIRE_IDENTITY_AUDIT_VALIDATION_IN_PRODUCTION` | `true` | 운영에서 identity audit validation 요구 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_REQUIRE_POLICY_CONFIG_IN_ENFORCING_MODE` | `true` | enforcing mode에서 policy config 요구 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_REQUIRE_FATAL_HANDLER_FAILURES_IN_PRODUCTION` | `true` | 운영에서 handler failure fatal 요구 |
| `PLATFORM_GOVERNANCE_OPERATIONAL_ALLOW_IGNORE_AUDIT_FAILURE_POLICY_IN_PRODUCTION` | `false` | 운영에서 audit failure ignore 허용 여부 |

## 감사 이벤트

| Event | Category | 주요 attributes |
| --- | --- | --- |
| `AUTH_LOGIN_PASSWORD` | `auth` | `eventType`, `result`, `actorId`, `channel` |
| `AUTH_LOGIN_SSO` | `auth` | `eventType`, `result`, `actorId`, `provider` |
| `AUTH_LOGOUT` | `auth` | `eventType`, `result`, `actorId`, `channel` |
| `AUTH_INTERNAL_ACCOUNT_CREATE` | `auth` | `eventType`, `actorType`, `resourceId`, `loginId` |
| `AUTH_INTERNAL_ACCOUNT_DELETE` | `auth` | `eventType`, `actorType`, `resourceId` |
