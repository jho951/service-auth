# Auth Docker

## Compose 파일

| 환경 | Compose 파일 | env 파일 | Spring 프로필 |
| --- | --- | --- | --- |
| `dev` | `docker/dev/compose.yml` | `.env.dev` | `dev` |
| `prod` | `docker/prod/compose.yml` | `.env.prod` | `prod` |

실행:

```bash
./scripts/run.docker.sh up dev app
./scripts/run.docker.sh up prod app
```

종료:

```bash
./scripts/run.docker.sh down dev app
./scripts/run.docker.sh down prod app
```

루트 env 파일이 없으면 실행을 중단합니다.

## 디렉토리 구조

Docker runtime 파일은 환경 단위로 관리합니다.

```text
db
└── schema.sql
docker
├── Dockerfile
├── dev
│   ├── compose.yml
│   └── services
│       └── mysql
│           ├── init.sql
│           └── my.cnf
└── prod
    ├── compose.yml
    └── services
        └── mysql
            ├── init.sql
            └── my.cnf
```

`docker/Dockerfile`은 dev/prod가 공유합니다. Compose와 service config는 `docker/{env}` 아래에 둡니다.

## env 파일 결정

`scripts/run.docker.sh`는 실행 환경에 맞는 루트 env 파일을 사용합니다.

| 환경     | env 파일      |
|--------|-------------|
| `dev`  | `.env.dev`  |
| `prod` | `.env.prod` |

env 파일이 없으면 실행을 중단합니다. 새 환경 파일은 `.env.example`을 기준으로 만듭니다.

## 서비스

| 서비스            | 이미지 또는 빌드                 | 네트워크                             | 비고                       |
|----------------|---------------------------|----------------------------------|--------------------------|
| `auth-service` | `docker/Dockerfile` build | `service-shared`, `auth-private` | Spring Boot auth-service |
| `auth-mysql`   | `mysql:8.0`               | `auth-private`                   | auth-service 전용 MySQL    |

## 네트워크

- `SERVICE_SHARED_NETWORK` external network
  - gateway, auth-service, user-service, 중앙 Redis가 함께 붙는 공유 네트워크입니다.
  - Compose 내부 alias는 `service-shared`입니다.
  - 기본 실제 네트워크 이름은 `service-backbone-shared`입니다.
- `auth-private` internal network
  - `auth-service`와 `auth-mysql`만 붙는 private network입니다.
  - MySQL은 공유 네트워크에 노출하지 않습니다.
- `service-backbone-shared` external network
  - `SERVICE_SHARED_NETWORK`의 기본값입니다.
  - 중앙 Redis 및 서비스 간 통신의 기본 backbone입니다.

구성 의도:

- 서비스 간 HTTP 호출과 중앙 Redis 접근은 external shared network를 사용합니다.
- auth 전용 DB 접근은 internal private network로 제한합니다.
- 다른 서비스가 auth DB에 직접 붙지 않도록 DB는 `auth-private`에만 둡니다.

## 네트워크 이름 결정

`scripts/run.docker.sh`는 `up` 실행 시 공유 네트워크 이름을 아래 우선순위로 결정합니다.

```text
SHARED_SERVICE_NETWORK
BACKEND_SHARED_NETWORK
SERVICE_SHARED_NETWORK
service-backbone-shared
```

결정된 네트워크가 없으면 스크립트가 `docker network create`로 external network를 먼저 생성합니다.

Compose 파일에서는 아래 값으로 external network를 참조합니다.

```yaml
networks:
  service-shared:
    external: true
    name: ${SERVICE_SHARED_NETWORK:-service-backbone-shared}
```

## 환경 변수

주요 Docker runtime 변수:

| 변수 | 기본값 | 용도 |
| --- | --- | --- |
| `SERVICE_SHARED_NETWORK` | `service-backbone-shared` | external shared network 이름 |
| `SERVER_PORT` | dev Compose 기준 `8081` | 컨테이너 내부 애플리케이션 포트 |
| `AUTH_SERVICE_HOST_PORT` | `8082` | dev Compose에서 호스트에 노출하는 포트 |
| `MYSQL_HOST` | env 파일 값 | MySQL 호스트명. Docker에서는 보통 `auth-mysql` |
| `MYSQL_DB` | env 파일 값 | MySQL 데이터베이스 이름 |
| `MYSQL_USER` | env 파일 값 | MySQL 애플리케이션 사용자 |
| `MYSQL_PASSWORD` | env 파일 값 | MySQL 애플리케이션 비밀번호 |
| `MYSQL_ROOT_PASSWORD` | env 파일 값 | MySQL root 비밀번호 |
| `REDIS_HOST` | env 파일 값 | shared network에 붙은 중앙 Redis 호스트명 |
| `REDIS_PORT` | env 파일 값 | 중앙 Redis 포트 |
| `GITHUB_ACTOR` | 빈 값 | private GitHub Packages build 인증 사용자 |
| `GITHUB_TOKEN` | 빈 값 | private GitHub Packages build 인증 토큰 |

`GITHUB_ACTOR`와 `GITHUB_TOKEN`은 Docker build 단계에서 Gradle이 `platform-governance`, `platform-security` private package를 받을 때 필요합니다.

`AUTH_ENV_FILE`은 스크립트가 Compose에 넘기는 내부 변수입니다. 직접 실행할 때만 override합니다.

dev Compose는 기본적으로 `localhost:8082`를 auth-service 컨테이너의 `SERVER_PORT`에 연결합니다.

```text
http://localhost:8082/swagger-ui.html
```

## 볼륨과 설정

MySQL data volume:

```text
auth-mysql-volume
```

MySQL config/init 파일:

| 환경 | `my.cnf` | bootstrap SQL | schema SQL |
| --- | --- | --- | --- |
| `dev` | `docker/dev/services/mysql/my.cnf` | `docker/dev/services/mysql/init.sql` | `db/schema.sql` |
| `prod` | `docker/prod/services/mysql/my.cnf` | `docker/prod/services/mysql/init.sql` | `db/schema.sql` |

역할 분리:

```text
docker/{dev,prod}/services/mysql/init.sql
  DB 생성
  사용자/권한 생성
  USE <database>
  SOURCE /schema/auth-schema.sql

db/schema.sql
  auth_accounts
  auth_login_attempts
  mfa_factors
```

Compose는 repo 루트의 `db/schema.sql`을 MySQL 컨테이너의 `/schema/auth-schema.sql`로 read-only mount합니다. 테이블 DDL은 `db/schema.sql`만 수정하고, Docker init SQL에 복사하지 않습니다.

`dev down`은 `--remove-orphans -v`를 사용하므로 named volume도 삭제됩니다.

```bash
./scripts/run.docker.sh down dev app
```

`prod down`은 volume을 삭제하지 않습니다.

```bash
./scripts/run.docker.sh down prod app
```

운영 데이터가 있는 환경에서는 Docker volume 삭제 명령을 별도로 실행하지 않습니다.

## 빌드 흐름

`docker/Dockerfile`은 multi-stage build입니다.

1. `gradle:8.5-jdk17`에서 의존성 확인과 `:app:build -x test`를 실행합니다.
2. `amazoncorretto:17-alpine-jdk` runtime image에 app jar만 복사합니다.
3. 기본 entrypoint는 `java -jar app.jar`입니다.

Compose가 profile 인자를 넘깁니다.

```yaml
command: ["--spring.profiles.active=dev"]
```

## 운영 확인

공유 네트워크 확인:

```bash
docker network inspect service-backbone-shared
```

컨테이너 상태 확인:

```bash
docker compose -p auth-service -f docker/dev/compose.yml ps
```

로그 확인:

```bash
docker compose -p auth-service -f docker/dev/compose.yml logs -f auth-service
```

DB healthcheck가 실패하면 먼저 `.env.dev` 또는 `.env.prod`의 MySQL 값과 `auth-mysql` 로그를 확인합니다.

## 변경 규칙

- 새 서비스가 auth DB에 직접 접근해야 한다면 먼저 구조를 재검토합니다. 기본 정책은 auth DB direct access 금지입니다.
- Compose 파일은 `docker/dev/compose.yml`, `docker/prod/compose.yml`을 기준으로 변경합니다.
- auth 테이블 schema는 `db/schema.sql`만 수정합니다. Docker `init.sql`에는 테이블 DDL을 중복 작성하지 않습니다.
- `.env.dev`/`.env.prod` 기본값을 바꾸면 `docker/{dev,prod}/compose.yml`과 이 문서를 같이 확인합니다.
- 공유 네트워크 이름을 바꾸면 gateway, user-service, 중앙 Redis Compose 설정도 함께 맞춰야 합니다.
- Redis는 이 레포에서 띄우지 않습니다. shared network에 이미 연결된 중앙 Redis endpoint를 환경변수로 주입합니다.
