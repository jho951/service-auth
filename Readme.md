# Auth-server

> *인증/인가 서버* 입니다.
> 
> MSA에서 독립 배포되는 `auth-service`이며, *로그인, 토큰 발급/재발급, 로그아웃, 인증 필터 연동*을 담당합니다.

## Architecture

- `auth-service`
  - login
  - refresh rotation
  - logout
  - access token authentication
- `app stack`
  - `mysql`
  - external `redis`
- `modules`
  - `app`: 실행 모듈
  - `common`: 공통 설정/응답/로깅 모듈

## 실행 가이드

### 기본 환경은 `dev`이며, shell을 통해 관리합니다.

```bash
./docker/start.sh
```

### 스택별 실행
```bash
./docker/start.sh dev app
./docker/start.sh dev all
```

### 종료
```bash
./docker/shutdown.sh dev app
./docker/shutdown.sh dev all
```

## Redis Endpoint Examples

이 서비스는 중앙 Redis endpoint를 환경변수로 받아 연결합니다.

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

## Notes

- 서비스 책임 분리와 `user-service` 설계안은 [docs/auth-user-service-design.md](./docs/auth-user-service-design.md) 문서를 참고하면 됩니다.
- 현재 Gradle 루트는 멀티모듈 집계 프로젝트이며, `app`과 `common` 모듈로 구성됩니다.
- Docker 환경 값의 단일 소스는 `gradle.properties`이고, `docker/*.sh`가 실행 시 `.generated/.env.*`를 생성합니다.
- Redis는 이 레포에서 별도 컨테이너로 띄우지 않으며, 중앙 Redis endpoint를 환경변수로 주입받아 사용합니다.
- 의존성 및 플러그인 버전은 `gradle/libs.versions.toml`에서 중앙 관리합니다.
- SSO 시작 API는 `page=explain|editor|admin` 또는 각 페이지의 등록된 `redirect_uri`를 기준으로 동작합니다.
- GitHub OAuth 적용은 `io.github.jho951 auth` `1.1.4`의 OAuth2 모델/SPI와 Spring Security OAuth2 Client를 사용하고, 성공 후에는 기존과 동일하게 일회용 `ticket`을 발급해 `/auth/exchange`로 세션을 만듭니다.
- `admin` 페이지는 `ip-guard` 화이트리스트를 통과해야만 SSO 시작/교환/세션확인이 허용됩니다.
