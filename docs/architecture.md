# Architecture

## Overview

`auth-service`는 인증 업무 로직과 플랫폼 결합 지점을 분리하기 위해 `app`과 `common` 두 모듈로 나뉩니다. 목적은 인증 도메인 로직을 Spring Security, Redis, Swagger 같은 공통 인프라 세부사항과 섞지 않는 것입니다.

## Why It Is Split This Way

- 인증 도메인 로직은 `app`에 모아 두고 서비스 내부 공통 인프라는 `common`으로 제한합니다.
- `platform-security`, `platform-governance`와의 결합은 adapter/config 계층으로 모아서 도메인 규칙이 플랫폼 구현 타입에 잠기지 않게 합니다.
- 외부 연동 경계인 `user-service`, GitHub OAuth, Redis store를 명확히 분리해 테스트와 교체 비용을 낮춥니다.

## Module Boundaries

| Module | Role | Contains | Does Not Contain |
| --- | --- | --- | --- |
| `app` | 실행 애플리케이션과 인증 업무 로직 | controller, service, security config, user-service client, SSO flow | 여러 서비스가 공유할 범용 유틸 |
| `common` | 서비스 내부 공통 인프라 | 공통 예외, 응답 래퍼, logging, Redis config, Swagger config, web util | auth 도메인 규칙, 외부 서비스별 비즈니스 로직 |
| `db` | DB baseline source | `schema.sql` | 애플리케이션 코드 |
| `docker` | local/prod compose 자산 | compose, Dockerfile, MySQL bootstrap | 배포 orchestration 로직 |
| `deploy` | EC2 bundle 자산 | remote compose, env template, schema copy | 앱 source of truth |
| `infra` | Terraform/AWS 자산 | IaC | 런타임 로직 |

## Layer Structure

요청 처리는 대체로 아래 순서를 따릅니다.

```text
HTTP request
-> logging/security filter
-> controller
-> domain service
-> repository or outbound client/store
-> response writer / cookie writer
```

## Package Structure

### `app/src/main/java/com/authservice/app`

| Package | Role |
| --- | --- |
| `config.logging` | 요청 MDC와 access log filter |
| `config.status` | 상태 확인 endpoint |
| `domain.auth` | password login, refresh, auth account 정책과 persistence |
| `domain.auth.internal` | 내부 서비스 간 auth account API |
| `domain.auth.sso` | OAuth 시작, callback, ticket, browser session |
| `domain.auth.userdirectory` | `user-service` 원격 사용자 연동 |
| `domain.auth.support` | cookie, token, UUID 등 auth 전용 helper |
| `domain.audit` | governance audit adapter |
| `security` | Spring Security와 `platform-security` 조립 |

### `common/src/main/java/com/authservice/common`

| Package | Role |
| --- | --- |
| `base` | `GlobalResponse`, 공통 예외, base entity |
| `logging` | logging header/MDC key, 민감정보 마스킹 |
| `redis` | Redis connection과 typed store |
| `swagger` | OpenAPI/Swagger 설정 |
| `web` | client IP resolver, error endpoint |

## Dependency Direction

- `app -> common`
- controller는 service로만 위임하고 핵심 판단은 service가 수행합니다.
- `domain.auth.userdirectory`가 `user-service` 경계를 소유합니다.
- `security` 패키지가 `platform-security`와의 직접 결합을 소유합니다.
- audit adapter는 `domain.audit`와 `security` 경계에만 둡니다.
- `common`은 `app`를 참조하지 않습니다.

## Platform Usage

현재 구현은 platform을 두 층으로 사용합니다.

- 도메인 층: auth login, refresh, SSO, internal account, audit 의미 결정
- 플랫폼 adapter 층: token/session issuer, internal auth bridge, rate limit, governance audit sink

이 분리 덕분에 auth-service는 인증 정책을 직접 소유하면서도 cross-cutting security/governance 기능은 platform starter를 재사용합니다.

## Package Placement Rules

- 새 public/internal 인증 API DTO는 해당 도메인 하위 `dto` 패키지에 둡니다.
- 새 외부 호출은 기존 경계 뒤에 두고 controller/service에서 raw HTTP client를 직접 만들지 않습니다.
- 공통 로깅/예외/웹 보조 기능만 `common`으로 올립니다.
- DB baseline 변경은 항상 `db/schema.sql`을 source of truth로 사용합니다.

## System Context

```text
Browser / API Client
        |
        v
 gateway-service
        |
        v
   auth-service
    |    |    |
    |    |    +--> MySQL
    |    +-------> Redis
    +------------> user-service
        |
        +------------> GitHub OAuth

Adjacent responsibility:
authz-service owns authorization, not authentication
```

## Inbound And Outbound Boundaries

| Direction | Peer | Purpose |
| --- | --- | --- |
| Inbound | `gateway-service` | `/auth/*` public upstream route 전달 |
| Inbound | `gateway-service` | `/internal/auth/session/validate` 내부 session 검증 |
| Inbound | `user-service` or `gateway-service` | 내부 auth account 생성/삭제 |
| Outbound | `user-service` | 사용자 조회, internal account user 연계 |
| Outbound | GitHub OAuth | SSO 인증 |
| State | Redis | SSO state/ticket/session, refresh token 메타데이터, cache, prod rate-limit |
| State | MySQL | auth account, login attempts, MFA factor 저장 |

## Gateway Mapping

| Public Route | Upstream Route |
| --- | --- |
| `POST /v1/auth/login` | `POST /auth/login` |
| `POST /v1/auth/refresh` | `POST /auth/refresh` |
| `POST /v1/auth/logout` | `POST /auth/logout` |
| `GET /v1/auth/sso/start` | `GET /auth/sso/start` |
| `POST /v1/auth/exchange` | `POST /auth/exchange` |
| `GET /v1/auth/session` | `GET /auth/session` |
| `GET /v1/auth/me` | `GET /auth/me` |
| `GET /v1/login/oauth2/code/github` | `GET /login/oauth2/code/github` |

## Boundary Rules

- Gateway가 public versioning을 소유하고 auth-service는 upstream contract를 소유합니다.
- `auth-service`는 사용자 프로필 원본을 저장하지 않고 `user-service`를 source of truth로 사용합니다.
- 인증 성공 결과는 session/JWT/cookie 형태로 전달되고 최종 인가는 downstream 또는 `authz-service`가 담당합니다.
- internal route는 public route와 별도 trust boundary를 가집니다.
