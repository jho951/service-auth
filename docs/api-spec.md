# API Spec

## Canonical Spec And Contract Source

- Local OpenAPI copy: [openapi/auth-service.yml](./openapi/auth-service.yml)
- Dev Swagger UI: `http://localhost:8081/swagger-ui.html`
- Contract repo: `https://github.com/jho951/service-contract`
- Lock file: [`contract.lock.yml`](../contract.lock.yml)

`auth-service`는 public API versioning을 직접 소유하지 않습니다. 아래 문서는 upstream route 기준입니다.

## Endpoint Groups

### Status And Discovery

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/` | 서비스 상태 확인 |
| `GET` | `/error` | Spring error endpoint |
| `GET` | `/.well-known/jwks.json` | JWT 검증용 public key set |

### Password Auth

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/auth/login` | username/password 로그인 |
| `POST` | `/auth/refresh` | refresh token 기반 토큰 재발급 |
| `POST` | `/auth/logout` | cookie/session 정리와 로그아웃 |

### SSO

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/auth/sso/start` | GitHub SSO 시작 |
| `GET` | `/auth/login/github` | SSO 시작 alias |
| `GET` | `/auth/oauth2/authorize/github` | SSO 시작 alias |
| `GET` | `/auth/oauth/github/callback` | legacy callback redirect |
| `GET` | `/login/oauth2/code/github` | Spring Security OAuth callback |
| `POST` | `/auth/exchange` | SSO ticket 교환 |
| `GET` | `/auth/session` | browser session 검증 |
| `GET` | `/auth/me` | 현재 사용자 요약 |

### Internal

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/internal/auth/session/validate` | Gateway용 session 검증 |
| `POST` | `/internal/auth/accounts` | 내부 auth account 생성 |
| `DELETE` | `/internal/auth/accounts/{userId}` | 내부 auth account 삭제 |

## Contract Surface

현재 auth-service가 보장하는 주요 contract는 아래 네 가지입니다.

- upstream HTTP API shape
- shared header와 trust boundary 규칙
- session/token 발급과 검증 의미
- versioning과 변경 절차

Public client가 보는 `/v1/auth/*`는 Gateway contract이며 auth-service 구현 contract와 분리됩니다.

## Request And Response Shape

- 로그인과 refresh는 token pair 응답을 사용합니다.
- SSO exchange는 cookie/session 설정 중심 응답입니다.
- internal account 생성은 `GlobalResponse` 래퍼를 사용합니다.
- 세부 필드와 예시는 OpenAPI 파일을 source of truth로 봅니다.

대표 DTO 가족:

- `AuthRequest.LoginRequest`
- `AuthResponse.TokenResponse`
- `SsoRequest.ExchangeRequest`
- `SsoResponse.InternalSessionValidationResponse`
- `InternalAuthRequest.CreateAccountRequest`
- `InternalAuthResponse.AccountResponse`

일부 internal admin 응답은 `GlobalResponse` wrapper를 사용합니다.

## Shared Headers

공유 헤더는 `common/logging/LoggingHeaders`와 shared contract를 함께 따릅니다.

- `X-Request-Id`
- `X-Correlation-Id`
- `traceparent`
- `X-Forwarded-For`

이 헤더는 로그 상관관계, trace 추적, 실제 client IP 복원에 사용됩니다.

## Errors

- HTTP status, response schema, endpoint별 예외는 [openapi/auth-service.yml](./openapi/auth-service.yml)을 기준으로 봅니다.
- 서비스 에러 의미는 구현 코드와 service-contract의 shared error 규칙을 함께 따릅니다.

## Event Contract

현재 auth-service는 외부 consumer가 구독하는 public 비동기 이벤트 contract를 제공하지 않습니다.

- 감사 이벤트는 `platform-governance` audit sink로 기록됩니다.
- audit entry는 운영/감사 목적이며 외부 integration event schema로 약속된 것은 아닙니다.

## Versioning Rule

- 외부 client용 `/v1/auth/*` 버전은 Gateway가 관리합니다.
- auth-service upstream route는 현재 non-versioned입니다.
- breaking change는 service-contract와 OpenAPI를 먼저 갱신한 뒤 구현에 반영합니다.

## Change Rules

1. 계약 변경이 필요하면 service-contract를 먼저 갱신합니다.
2. `contract.lock.yml`과 `docs/openapi/auth-service.yml`을 같이 맞춥니다.
3. 구현 코드와 테스트를 갱신합니다.
4. breaking change는 `CHANGELOG.md`에 기록합니다.
