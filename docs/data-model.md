# Data Model

## Overview

- MySQL: 계정과 이력의 durable state
- Redis: 세션성/캐시성/짧은 TTL state

## Tables

| Table | Purpose |
| --- | --- |
| `auth_accounts` | 로그인 계정, password hash, 잠금 상태, 실패 횟수 |
| `auth_login_attempts` | 로그인 성공/실패 이력 append-only 로그 |
| `mfa_factors` | MFA factor 메타데이터 |

`auth-service`는 `auth_audit_logs` 같은 별도 JPA 엔티티를 관리하지 않습니다. 감사 이벤트는 governance audit sink로 전송됩니다.

## Logical Relationships

```text
user-service user (external source of truth)
        |
        +-- 1:1-ish --> auth_accounts.user_id
        |
        +-- 1:N -----> mfa_factors.user_id

auth_accounts.login_id
        |
        +-- 1:N -----> auth_login_attempts.login_id
```

DB 레벨 foreign key는 두지 않습니다. 이유는 `user_id` 원본이 `user-service`에 있고, 서비스 간 결합을 DB foreign key로 만들지 않기 위해서입니다.

## Key Columns And Indexes

| Table | Column | Notes |
| --- | --- | --- |
| `auth_accounts` | `id` | PK, `CHAR(36)` UUID |
| `auth_accounts` | `user_id` | unique, user-service user id |
| `auth_accounts` | `login_id` | unique, 로그인 식별자 |
| `auth_login_attempts` | `id` | PK, canonical UUID string |
| `mfa_factors` | `id` | PK, `CHAR(36)` UUID |
| `mfa_factors` | `user_id` | indexed for user lookup |

현재 스키마 인덱스:

- `uk_auth_accounts_user_id`
- `uk_auth_accounts_login_id`
- `ix_mfa_factors_user_id`

## UUID Policy

- DB type: `CHAR(36)`
- 저장 형식: canonical UUID string with hyphen
- Java type: 주로 `UUID`
- baseline source: [`db/schema.sql`](../db/schema.sql)

이 정책은 서비스 간 식별자 표현을 단순하게 유지하고 raw binary UUID 해석 차이를 피하기 위한 선택입니다.

## Transaction Boundaries

- password login은 `auth_accounts` 조회/수정, login attempt 기록, token 발급이 한 요청 흐름 안에서 일어납니다.
- refresh는 토큰 검증과 Redis refresh token 메타데이터 갱신이 함께 움직입니다.
- internal account 생성/삭제는 `auth_accounts` 변경과 audit 기록이 같이 수행됩니다.
- SSO state/ticket/session은 Redis 쪽 임시 상태이므로 MySQL transaction 경계 밖에 있습니다.

## Source Of Truth Rules

- 신규 테이블/컬럼 baseline은 항상 `db/schema.sql`에 먼저 반영합니다.
- Docker MySQL init SQL은 bootstrap만 담당하고 테이블 DDL을 중복 소유하지 않습니다.
- 운영 DB schema 변경 기준은 migration tool이 아니라 현재 baseline SQL과 배포 동기화 스크립트입니다.

## Redis State Model

Redis는 durable source가 아니라 빠른 임시 상태 저장소입니다.

- SSO OAuth state
- SSO one-time ticket
- browser session payload
- refresh token 메타데이터
- user lookup cache
- prod profile rate-limit backend

### Key Spaces

| Purpose | Prefix | Notes |
| --- | --- | --- |
| OAuth state | `auth:oauth-state:` | legacy read prefix `oauth:state:` 호환 |
| SSO ticket | `auth:ticket:` | one-time consume |
| SSO session | `auth:session:` | legacy read prefix `sso:session:` 호환 |
| Refresh token metadata | `refresh:jti:` | token hash 기준 |
| Refresh token user index | `refresh:user:` | `userId:hash` 형태 |
| Cached auth user | `auth:user:` | username 기준 |
| Rate limit | `platform-security:rate-limit:auth-service:` | prod bean에서 사용 |

### TTL Rules

| Data | TTL Source |
| --- | --- |
| OAuth state | `SSO_STATE_TTL_SECONDS`, 기본 300초 |
| SSO ticket | `SSO_TICKET_TTL_SECONDS`, 기본 120초 |
| SSO session | `SSO_SESSION_TTL_SECONDS`, 기본 604800초 |
| Refresh token metadata | auth refresh token 만료 시각 |
| User cache | `AUTH_USER_CACHE_TTL_SECONDS`, 기본 300초 |
| Rate-limit bucket | route/window 설정값 |

### Failure Behavior

- user cache 실패: cache miss로 처리
- refresh token metadata 실패: 예외를 삼키고 auth flow 지속
- SSO state/session 저장 실패: 경고 로그 후 계속 진행
- SSO read/delete 실패: cache miss 또는 best-effort revoke

Redis 장애가 길어지면 session/SSO/refresh 동작 품질은 저하될 수 있습니다.
