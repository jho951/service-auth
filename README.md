# Auth-server

> MSA에서 독립 배포되는 `auth-service`입니다.

## Contract Source

- 공통 계약 레포: `https://github.com/jho951/contract`
- 이 서비스의 코드 SoT: `Auth-server` `main`
- 인터페이스 변경 시 본 저장소 구현보다 계약 레포 변경을 먼저 반영합니다.
- 책임 분리: `Auth-server`는 인증/세션/토큰, `Authz-server`는 capability 진실, `User-server`는 프로필 공개 범위를 소유합니다.

## Architecture

- `auth-service`
  - login
  - refresh rotation
  - logout
  - cookie/session + access token authentication
- 현재 구현은 외부 `auth` 모듈을 통해 인증 원천과 세션 발급 흐름을 구성한다.
- `app stack`
  - `mysql`
  - external `redis`
- `modules`
  - `app`: 실행 모듈
  - `common`: 공통 설정/응답/로깅 모듈
- Docker 네트워크 구조
  - `SERVICE_SHARED_NETWORK`(external): gateway/auth/user-service 간 서비스 통신용 공유 네트워크
    - 기본값: `service-backbone-shared`
  - `auth-private`(internal): auth-service와 auth-mysql 전용 private 네트워크
  - `service-backbone-shared`(external): 중앙 Redis 및 서비스 간 공유 네트워크

## 실행 가이드

### 기본 환경은 `dev`이며, shell을 통해 관리합니다.

```bash
./scripts/run.docker.sh
```

`./scripts/run.docker.sh`는 `.env.dev`/`.env.prod`가 없어도 로컬 기본값으로 실행됩니다.
운영처럼 강제 검증이 필요하면 `--strict` 옵션을 사용합니다.

```bash
./scripts/run.docker.sh up dev app --strict
```

### 스택별 실행
```bash
./scripts/run.docker.sh up dev app
./scripts/run.docker.sh up dev all
```

### 종료
```bash
./scripts/run.docker.sh down dev app
./scripts/run.docker.sh down dev all
```

### 로컬 실행
```bash
./scripts/run.local.sh
./scripts/run.local.sh dev
./scripts/run.local.sh prod
```

### dev app 초기화 후 재기동
```bash
./scripts/run.docker.sh down dev app
./scripts/run.docker.sh up dev app
```

## Redis Endpoint Examples

이 서비스는 중앙 Redis endpoint를 환경변수로 받아 연결합니다.
현재 `dev`/`prod` 기본값과 Gradle 설정은 `central-redis`를 사용합니다.

### 로컬에서 auth-service만 실행

```bash
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=your-redis-password
export REDIS_SSL=false
```

### 같은 Docker host에서 별도 Redis compose 사용

```bash
export REDIS_HOST=central-redis
export REDIS_PORT=6379
export REDIS_PASSWORD=your-redis-password
export REDIS_SSL=false
```

### 운영 환경에서 private DNS 사용

```bash
export REDIS_HOST=redis.internal.company
export REDIS_PORT=6379
export REDIS_PASSWORD=your-redis-password
export REDIS_SSL=false
```

## Database

- 상세 DB 문서는 [docs/database.md](/Users/jhons/Downloads/BE/Auth-server/docs/database.md)를 참고하면 됩니다.
- 현재 애플리케이션이 관리하는 주요 테이블은 `auth_accounts`, `auth_login_attempts`, `mfa_factors` 입니다.
- 감사 로그 테이블 `auth_audit_logs` 는 제거되었고 현재 코드에서 사용하지 않습니다.
- `dev` 프로필은 JPA `ddl-auto: create` 로 애플리케이션 시작 시 스키마를 생성합니다.
- `prod` 프로필은 JPA `ddl-auto: none` 이므로 스키마를 사전에 준비해야 합니다.
- UUID 식별자는 코드와 DB 모두 `CHAR(36)` 바인딩을 사용합니다.
- 운영 DB가 이전 `BINARY(16)` 스키마라면 `db/migrations/2026-03-31_bind_uuid_columns_char36.sql` 및 `scripts/db/apply-bind-uuid-char36-prod.sh`를 먼저 적용해야 합니다.
- 과거 소셜 계정 제거용 마이그레이션이 필요하면 `db/migrations/2026-03-27_drop_auth_social_accounts.sql` 및 `scripts/db/apply-drop-auth-social-accounts-prod.sh`를 참고하면 됩니다.

## Notes

- 서비스 책임 분리와 `user-service` 설계안은 [docs/auth-user-service-design.md](./docs/auth-user-service-design.md) 문서를 참고하면 됩니다.
- 현재 구조에서 `auth-service` 는 인증 컨텍스트를 만들고, `user-service` 는 사용자 마스터 데이터를 소유합니다. SSO 로그인 완료 시 `auth-service` 는 `USER_SERVICE_BASE_URL` 을 통해 `user-service` 를 호출합니다.
- 권한 보유 사실의 공개 여부는 auth-service가 아닌 user-service privacy/visibility 정책에서 다룹니다.
- DB 스키마와 환경별 생성 정책은 [docs/database.md](./docs/database.md) 문서를 참고하면 됩니다.
- 현재 Gradle 루트는 멀티모듈 집계 프로젝트이며, `app`과 `common` 모듈로 구성됩니다.
- Docker 환경 값의 단일 소스는 루트 `.env.dev`/`.env.prod`입니다.
- Redis는 이 레포에서 별도 컨테이너로 띄우지 않으며, 중앙 Redis endpoint를 환경변수로 주입받아 사용합니다.
- 의존성 및 플러그인 버전은 `gradle/libs.versions.toml`에서 중앙 관리합니다.
- 감사 로그는 `io.github.jho951:config:1.0.1`(`audit-log`)로 통일되며 기본 출력 파일은 `./logs/audit.log` 입니다.
- SSO 시작 API는 `page=explain|editor|admin` 또는 각 페이지의 등록된 `redirect_uri`를 기준으로 동작합니다.
- GitHub OAuth 적용은 `io.github.jho951 auth` `1.1.4`의 OAuth2 모델/SPI와 Spring Security OAuth2 Client를 사용하고, 성공 후에는 기존과 동일하게 일회용 `ticket`을 발급해 `/auth/exchange`로 세션을 만듭니다.
- GitHub OAuth callback URI는 gateway 기준 `/v1/login/oauth2/code/github` 와 일치해야 합니다. 예시 dev 값은 `http://localhost:8080/v1/login/oauth2/code/github` 입니다.
- 브라우저 클라이언트는 `credentials: 'include'` 와 쿠키만 사용하고, `Authorization` 은 비브라우저 fallback 입니다.
- `admin` 페이지는 `ip-guard` 화이트리스트를 통과해야만 SSO 시작/교환/세션확인이 허용됩니다.
- 로컬 개발(`dev`)에서는 `admin` SSO의 IP guard를 기본 비활성화합니다. 운영에서는 계속 활성화됩니다.
- 로컬에서 IP guard를 수동 활성화해야 한다면 `127.0.0.1`, `::1`, `192.168.65.1`, `172.17.0.1`, `172.16.0.0/12` 같은 Docker host gateway/bridge 대역을 허용 목록에 포함해야 합니다.
