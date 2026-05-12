# Auth Service

## 1. Overview

`auth-service`는 플랫폼의 인증 원천 서비스입니다. 사용자 인증, JWT 발급, refresh rotation, browser session, GitHub SSO 연동을 담당하며 외부 클라이언트는 직접 이 서비스를 호출하지 않고 보통 Gateway를 통해 접근합니다.

## 2. Responsibilities

- 이메일/비밀번호 로그인 처리
- access token, refresh token 발급과 재발급
- browser session 조회와 로그아웃 처리
- GitHub SSO 시작, callback, ticket 교환
- 내부 서비스용 auth account 생성/삭제
- Gateway의 내부 session 검증 지원
- 인증 감사 이벤트 기록

## 3. Role In The System

- 외부 public 경로는 Gateway가 `/v1/auth/*` 형태로 소유합니다.
- `auth-service`는 upstream 경로 `/auth/*`, `/internal/auth/*`, `/.well-known/jwks.json`를 소유합니다.
- 사용자 프로필과 계정 상태 원본은 `user-service`가 소유합니다.
- 최종 인가 판단은 `authz-service` 책임이며 `auth-service`는 인증 결과만 제공합니다.

## 4. Main Capabilities

- Password login: `POST /auth/login`
- Token refresh: `POST /auth/refresh`
- SSO start and exchange: `GET /auth/sso/start`, `POST /auth/exchange`
- Current session/user lookup: `GET /auth/session`, `GET /auth/me`
- Logout: `POST /auth/logout`
- Internal account management: `POST /internal/auth/accounts`, `DELETE /internal/auth/accounts/{userId}`
- Internal session validation: `POST /internal/auth/session/validate`

## 5. Dependencies

| Type | Dependency | Why |
| --- | --- | --- |
| Inbound | `gateway-service` | public auth route versioning, edge security, path rewrite |
| Outbound | `user-service` | 사용자 조회와 internal account 연동 |
| Outbound | GitHub OAuth | SSO provider |
| State | Redis | SSO state/ticket/session, refresh token 메타데이터, user cache |
| State | MySQL | auth account, login attempt, MFA factor 저장 |
| In-process | `platform-security` | token/session security boundary, rate limit, internal auth bridge |
| In-process | `platform-governance` | 감사 이벤트 기록과 운영 정책 |
| In-process | `platform-integrations` | governance/security bridge |

## 6. Run

### Local Java

`.env.local`을 준비한 뒤 실행합니다.

```bash
./scripts/run.local.sh
```

### Docker

개발 환경:

```bash
./scripts/run.docker.sh up dev app
```

운영 형식 검증:

```bash
./scripts/run.docker.sh up prod app
```

종료:

```bash
./scripts/run.docker.sh down dev app
./scripts/run.docker.sh down prod app
```

Swagger UI는 dev profile에서 `http://localhost:8081/swagger-ui.html`로 노출됩니다.

## 7. Environment Summary

- Runtime: `SPRING_PROFILES_ACTIVE`, `SERVER_PORT`
- Security: `AUTH_JWT_SECRET`, `INTERNAL_API_KEY`
- Data: `MYSQL_*`, `REDIS_*`
- SSO: `SSO_GITHUB_*`, `SSO_*_ORIGIN`, `SSO_*_CALLBACK_URI`
- Package access: `GH_ACTOR`, `GH_TOKEN`

GitHub Packages actor 값은 Gradle 해석 기준으로 `GH_ACTOR`를 사용합니다. Docker dev build까지 함께 쓸 때는 `GITHUB_ACTOR`도 같은 값으로 맞추는 편이 안전합니다.

## 8. Document Map
- [README.md](./docs/README.md)

## 9. Change Checklist

- contract 영향이 있으면 `service-contract`, `contract.lock.yml`, OpenAPI를 같이 갱신합니다.
- DB 변경은 `db/schema.sql`을 source of truth로 유지합니다.
- 보안/인증 흐름 변경은 관련 flow test와 security test를 같이 봅니다.
- 문서 구조 변경은 위 핵심 문서 안에서 먼저 흡수하고 새 파일 추가는 최소화합니다.
