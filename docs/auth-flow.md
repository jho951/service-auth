# Auth Flow

## Route Model

| Type | Path | Purpose |
| --- | --- | --- |
| Gateway public auth route | `/v1/auth/*` | 외부 client 진입 |
| auth-service upstream route | `/auth/*` | Gateway가 전달하는 인증 route |
| OAuth callback public route | `/v1/login/oauth2/code/github` | GitHub callback 등록 경로 |
| OAuth callback upstream route | `/login/oauth2/code/github` | Spring Security callback |
| Internal route | `/internal/auth/*` | service-to-service 내부 호출 |

## Password Login

```text
1. Client -> Gateway: POST /v1/auth/login
2. Gateway -> auth-service: POST /auth/login
3. auth-service가 username/password 검증
4. access token + refresh token 발급
5. browser 요청이면 cookie도 함께 설정
6. audit event와 login attempt 기록
```

핵심 특징:

- redirect가 없는 synchronous 로그인 흐름입니다.
- 인증 성공 후 token 발급까지 auth-service가 끝냅니다.
- 계정 정책과 로그인 실패 카운트는 auth-service가 직접 관리합니다.

## Refresh And Logout

### Refresh

```text
1. Client -> Gateway: POST /v1/auth/refresh
2. Gateway -> auth-service: POST /auth/refresh
3. auth-service가 refresh token을 cookie/header에서 추출
4. refresh token 검증과 rotation 수행
5. 새 token pair와 cookie 설정
```

### Logout

```text
1. Client -> Gateway: POST /v1/auth/logout
2. Gateway -> auth-service: POST /auth/logout
3. auth-service가 SSO session과 auth cookie를 정리
4. logout audit event 기록
```

## GitHub SSO

```text
1. Client -> Gateway: GET /v1/auth/sso/start
2. Gateway -> auth-service: GET /auth/sso/start
3. auth-service가 target page와 redirect URI를 검증하고 state 저장
4. 302 -> GitHub authorization
5. GitHub -> Gateway public callback
6. Gateway -> auth-service callback
7. auth-service가 사용자 확인 후 일회용 ticket 발급
8. Client -> Gateway: POST /v1/auth/exchange
9. Gateway -> auth-service: POST /auth/exchange
10. auth-service가 ticket을 session/token으로 교환
```

현재 alias 경로:

- `/auth/login/github`
- `/auth/oauth2/authorize/github`

## Session And Current User

| Path | Meaning |
| --- | --- |
| `GET /auth/session` | browser session 또는 JWT context를 검증 |
| `GET /auth/me` | 현재 principal 요약 반환 |
| `POST /internal/auth/session/validate` | Gateway용 내부 session validation |

## Why Auth And User Are Split

- `auth-service`는 인증과 credential lifecycle을 소유합니다.
- `user-service`는 사용자 프로필, 공개 범위, 상태의 원본을 소유합니다.
- 이 분리 덕분에 비밀번호/세션 정책과 프로필/도메인 데이터 변경을 독립적으로 진화시킬 수 있습니다.

즉 `auth-service`는 “누구인지 인증하는 일”을 맡고, `user-service`는 “그 사용자가 어떤 프로필과 상태를 갖는지”를 맡습니다.

## How Authz Fits

- `auth-service`는 access token, session, internal validation 결과를 제공합니다.
- 최종 권한 판단과 정책 결정은 `authz-service`가 담당합니다.
- 현재 auth-service가 authz-service를 직접 호출하지는 않지만, 책임 경계는 명확히 분리되어 있습니다.

## Why `/login/oauth2/code/github` Is Kept

이 경로는 비즈니스 API가 아니라 Spring Security OAuth callback convention입니다.

- framework 기본값과 바로 맞물립니다.
- GitHub App callback 등록과 Gateway rewrite가 단순합니다.
- `/auth/...`로 바꿀 수는 있지만 설정 결합과 운영 리스크가 커집니다.

## Security Model

`auth-service`는 token/session issuer 역할의 서비스이며 `platform-security` preset `ISSUER`를 중심으로 동작합니다.

핵심 보안 축:

- password login과 JWT issuance
- browser session과 refresh cookie
- internal route trust boundary
- IP guard와 rate limit
- governance audit와 운영 fail-fast

## Internal Boundary

- `/internal/**`는 public API가 아닙니다.
- 현재 구현은 internal token capability를 사용합니다.
- route access는 `ROLE_INTERNAL`과 internal proof 검증을 함께 요구합니다.
- prod에서는 internal key가 비어 있거나 local 기본값이면 기동하지 않도록 설계되어 있습니다.

## IP Guard And Rate Limit

IP 제한은 두 층으로 존재합니다.

- `platform.security.ip-guard.admin`
- `platform.security.ip-guard.internal`

추가로 SSO admin frontend entry에는 `sso.frontend.admin.ip-guard`가 별도로 있습니다.

주요 rate-limit 대상 route:

- `/auth/login`
- `/auth/login/github`
- `/auth/oauth2/authorize/github`
- `/auth/sso/start`
- `/auth/refresh`
- `/auth/exchange`

prod profile에서는 Redis 기반 rate-limit backend를 사용합니다.

## Cookie, Logging, Audit

- refresh/logout은 cookie 기반 요청을 고려합니다.
- `CookieCsrfOriginGuardFilter`가 `Origin`/`Referer`를 검증합니다.
- 민감 정보는 `SensitiveDataMasker`로 마스킹합니다.
- 로그인, SSO, logout, internal account 변경은 audit event로 남깁니다.
- MDC와 access log는 보안 사건 추적용 상관관계 정보를 남깁니다.
