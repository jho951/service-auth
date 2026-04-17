# Auth API

## 한눈에 보기

```txt
Client
  -> Gateway public route: /v1/auth/*
  -> Auth-service upstream: /auth/*
```

Auth-service does not own public API version prefixes.
Gateway maps public `/v1/auth/*` routes to auth-service `/auth/*` routes.

| 구분 | 경로 | 소유자 | 설명 |
| --- | --- | --- | --- |
| Public auth route | `/v1/auth/*`, `/v2/auth/*` | Gateway | 외부 client가 호출하는 versioned route |
| Auth upstream route | `/auth/*` | Auth-service | Gateway가 전달하는 인증 route |
| OAuth callback public route | `/v1/login/oauth2/code/github` | Gateway | GitHub App에 등록하는 callback URL |
| OAuth callback upstream route | `/login/oauth2/code/github` | Auth-service | Spring Security OAuth callback |
| Internal account route | `/internal/auth/accounts/*` | Auth-service | User-service 연동용 계정 route |
| Runtime route | `/` | Auth-service | 상태 확인 |
| Discovery route | `/.well-known/jwks.json` | Auth-service | JWT 공개키 |

## Endpoint

| Area | Method | Upstream path | Public path example | Response | Notes |
| --- | --- | --- | --- | --- | --- |
| Token | `POST` | `/auth/login` | `/v1/auth/login` | `200 TokenResponse` | password login |
| Token | `POST` | `/auth/refresh` | `/v1/auth/refresh` | `200 TokenResponse` | refresh token은 cookie에서 추출 |
| Token | `POST` | `/auth/logout` | `/v1/auth/logout` | `204 No Content` | SSO session revoke + refresh token revoke + auth cookie clear |
| SSO | `GET` | `/auth/sso/start` | `/v1/auth/sso/start` | `302 Redirect` | GitHub SSO 시작 |
| SSO | `GET` | `/auth/login/github` | `/v1/auth/login/github` | `302 Redirect` | GitHub SSO alias |
| SSO | `GET` | `/auth/oauth2/authorize/github` | `/v1/auth/oauth2/authorize/github` | `302 Redirect` | GitHub only |
| SSO | `GET` | `/auth/oauth/github/callback` | `/v1/auth/oauth/github/callback` | `302 Redirect` | legacy callback redirect |
| OAuth | `GET` | `/login/oauth2/code/github` | `/v1/login/oauth2/code/github` | `302 Redirect` | GitHub App callback |
| SSO | `POST` | `/auth/exchange` | `/v1/auth/exchange` | `204 No Content` | SSO ticket 교환 |
| Session | `POST` | `/auth/internal/session/validate` | internal | `200 SessionValidation` | Gateway 컨텍스트 재구성 |
| Session | `GET` | `/auth/session` | `/v1/auth/session` | `200 SessionValidation` | session 조회 alias |
| Session | `GET` | `/auth/me` | `/v1/auth/me` | `200 MeResponse` | 현재 사용자 요약 |
| Internal | `POST` | `/internal/auth/accounts` | internal | `201 GlobalResponse<Account>` | Auth 계정 생성 |
| Internal | `DELETE` | `/internal/auth/accounts/{userId}` | internal | `200 GlobalResponse<Void>` | Auth 계정 삭제 |
| Runtime | `GET` | `/` | upstream | `200 ServiceStatus` | 상태 확인 |
| Discovery | `GET` | `/.well-known/jwks.json` | upstream | `200 JWKS` | JWT 공개키 |

## Common

### TokenResponse

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi..."
}
```

### SessionValidation

```json
{
  "authenticated": true,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "role": "USER",
  "status": "ACTIVE",
  "sessionId": "session-123"
}
```

### GlobalResponse

```json
{
  "httpStatus": 201,
  "success": true,
  "message": "리소스 생성 성공",
  "code": 2,
  "data": {}
}
```

## Token APIs

### `POST /auth/login`

사용자 credential을 검증하고 token을 발급한다.

| 항목 | 값 |
| --- | --- |
| Upstream | `POST /auth/login` |
| Public example | `POST /v1/auth/login` |
| Request body | `username`, `password` |
| Success | `200 TokenResponse` |
| Cookies | browser 흐름에서 access/refresh cookie가 함께 설정될 수 있음 |
| Audit | login success/failure |

Request:

```json
{
  "username": "user@example.com",
  "password": "password1234"
}
```

Rules:

- `username`은 email 형식이다.
- `password`는 8~72자다.
- 계정 잠금/로그인 실패 횟수 정책이 적용될 수 있다.

### `POST /auth/refresh`

refresh token으로 새 token pair를 발급한다.

| 항목 | 값 |
| --- | --- |
| Upstream | `POST /auth/refresh` |
| Public example | `POST /v1/auth/refresh` |
| Request body | 없음 |
| Token source | refresh token cookie |
| Success | `200 TokenResponse` |
| Cookies | 새 token cookie가 설정될 수 있음 |

### `POST /auth/logout`

현재 인증 세션을 종료한다.

| 항목 | 값 |
| --- | --- |
| Upstream | `POST /auth/logout` |
| Public example | `POST /v1/auth/logout` |
| Request body | 없음 |
| Success | `204 No Content` |
| Audit | logout |

Current behavior:

- `sso_session` cookie가 있으면 session store에서 revoke한다.
- refresh token cookie가 있으면 서버 저장소에서 revoke한다.
- `sso_session` cookie를 제거한다.
- `ACCESS_TOKEN` cookie를 제거한다.
- refresh token cookie를 제거한다.

## SSO APIs

### SSO Flow

```txt
1. GET /auth/sso/start 또는 /auth/oauth2/authorize/github
2. GitHub OAuth redirect
3. callback 처리
4. 일회용 ticket 발급
5. POST /auth/exchange
6. sso_session + ACCESS_TOKEN + refresh cookie 설정
```

### `GET /auth/sso/start`

GitHub SSO 시작 기본 경로다.

| Query | Required | Description |
| --- | --- | --- |
| `page` | no | `explain`, `editor`, `admin` 같은 target page |
| `redirect_uri` | no | callback 이후 돌아갈 등록된 redirect URI |

Response:

- `302 Redirect`
- OAuth state cookie가 설정될 수 있다.

### `GET /auth/login/github`

GitHub SSO 시작 alias다. 동작은 `/auth/sso/start`와 같다.

### `GET /auth/oauth2/authorize/github`

GitHub OAuth 시작 경로다.

| 항목 | 값 |
| --- | --- |
| Current provider | `github` |
| Unsupported provider | 별도 route 없음 |
| Future providers | route와 provider client를 명시적으로 추가 |

### GitHub OAuth callback

GitHub App에는 Gateway public callback URL을 등록한다.

```txt
https://<gateway-domain>/v1/login/oauth2/code/github
```

Gateway는 callback을 auth-service upstream으로 전달한다.

```txt
GET /v1/login/oauth2/code/github
  -> GET /login/oauth2/code/github
```

Auth-service는 `/v1/login/oauth2/**`를 구현하지 않는다.

### `GET /auth/oauth/github/callback`

legacy GitHub OAuth callback을 Spring Security callback 경로로 연결한다.

Response:

- `302 Redirect`

### `POST /auth/exchange`

SSO ticket을 browser session/token으로 교환한다.

Request:

```json
{
  "ticket": "sso-ticket-value"
}
```

Response:

- `204 No Content`
- `sso_session`, `ACCESS_TOKEN`, refresh token cookie가 설정될 수 있다.
- 발급한 refresh token은 서버 저장소에 저장되어 refresh/logout 검증에 사용된다.

## Session APIs

### `POST /auth/internal/session/validate`

Gateway가 내부 사용자 컨텍스트를 만들기 위해 호출한다.

| 항목 | 값 |
| --- | --- |
| Caller | Gateway |
| Input | cookie session 또는 JWT authentication context |
| Success | `200 SessionValidation` |
| Failure | `401 SessionValidation(authenticated=false)` |

Unauthenticated response:

```json
{
  "authenticated": false,
  "userId": "",
  "role": "",
  "status": "",
  "sessionId": ""
}
```

### `GET /auth/session`

현재 browser session을 조회하는 alias다.

Response:

- `/auth/internal/session/validate`와 같은 shape.

### `GET /auth/me`

현재 사용자 요약을 반환한다.

Response:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "name": "Jane Doe",
  "avatarUrl": "https://example.com/avatar.png",
  "roles": ["USER"],
  "status": "ACTIVE"
}
```

## Internal Account APIs

### `POST /internal/auth/accounts`

User-service가 사용자 생성 이후 Auth-service 계정을 만들 때 호출한다.

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `userId` | UUID | yes | User-service user id |
| `loginId` | email string | yes | 로그인 ID |
| `password` | string | yes | 평문 password, 8~72자 |

Request:

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "loginId": "user@example.com",
  "password": "password1234"
}
```

Response:

```json
{
  "httpStatus": 201,
  "success": true,
  "message": "리소스 생성 성공",
  "code": 2,
  "data": {
    "authId": "7f8b9a01-1111-4444-9999-0a0b0c0d0e0f",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "loginId": "user@example.com"
  }
}
```

### `DELETE /internal/auth/accounts/{userId}`

User-service 계정 삭제 또는 rollback 시 Auth-service 계정을 제거한다.

| Path variable | Type | Description |
| --- | --- | --- |
| `userId` | UUID | 삭제할 User-service user id |

Response:

```json
{
  "httpStatus": 200,
  "success": true,
  "message": "리소스 삭제 성공",
  "code": 4,
  "data": null
}
```

## Runtime / Discovery APIs

### `GET /`

서비스 상태 확인용이다.

```json
{
  "service": "auth-service",
  "status": "UP"
}
```

### `GET /.well-known/jwks.json`

JWT 검증용 공개키 집합을 제공한다.

```json
{
  "keys": [
    {
      "kty": "RSA",
      "use": "sig",
      "alg": "RS256",
      "kid": "auth-jwt-key",
      "n": "base64url-modulus",
      "e": "AQAB"
    }
  ]
}
```

Notes:

- RSA 계열 알고리즘과 public key가 설정되어 있을 때 key를 반환한다.
- RSA가 아니거나 public key가 없으면 `keys: []`를 반환한다.
- Gateway는 이 공개키로 Auth-service JWT 서명을 검증할 수 있다.

## Response Policy

| API group | Success response | Error response |
| --- | --- | --- |
| Token | plain JSON body + cookies | `AuthErrorResponse` |
| SSO start/callback | redirect | `AuthErrorResponse` |
| SSO exchange/logout | `204 No Content` + cookies | `AuthErrorResponse` |
| Session | plain JSON body | `AuthErrorResponse` 또는 unauthenticated session body |
| Internal account | `GlobalResponse` | `AuthErrorResponse` |
| Runtime/JWKS | plain JSON body | default error handling |

Gateway는 public 응답을 표준화할 수 있지만, Auth-service 내부 운영 기록은 Auth-service 원형 error code를 기준으로 한다.
