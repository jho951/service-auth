# Deployment

## Source Of Truth

| Area                  | Source                                              |
|-----------------------|-----------------------------------------------------|
| Local Java run        | `scripts/run.local.sh`                              |
| Docker run            | `scripts/run.docker.sh`                             |
| Base compose          | `docker/compose.yml`                                |
| Environment overrides | `docker/dev/compose.yml`, `docker/prod/compose.yml` |
| Docker build override | `docker/compose.build.yml`                          |
| EC2 bundle            | `deploy/ec2`                                        |
| CI/CD profile         | `contract.lock.yml`                                 |

## Local Run

### Java

`.env.local`을 만든 뒤 실행합니다.

```bash
./scripts/run.local.sh
```

실행 profile은 기본적으로 `dev`입니다.

### Docker

개발:

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

## Compose Structure

| File | Role |
| --- | --- |
| `docker/compose.yml` | 공통 런타임 뼈대 |
| `docker/dev/compose.yml` | dev 차이 |
| `docker/prod/compose.yml` | prod 차이 |
| `docker/compose.build.yml` | dev 로컬 build secret, image build |

실행 조합:

- `dev`: base + dev override + build override
- `prod`: base + prod override

## Runtime Topology

서비스와 네트워크는 현재 아래 형태를 가정합니다.

| Component | Role |
| --- | --- |
| `auth-service` | Spring Boot runtime |
| `auth-mysql` | auth 전용 DB |
| `service-shared` | Gateway, user-service, central Redis와 통신 |
| `auth-private` | auth-service와 auth-mysql 내부 통신 |

## Build And Secrets

`docker/Dockerfile`은 multi-stage build를 사용합니다.

1. Gradle image에서 dependency resolve
2. `:app:build -x test`
3. runtime image에 jar만 복사

private GitHub Packages 접근은 build-time에만 필요합니다.

- `GH_ACTOR`
- `GH_TOKEN`

현재 Docker helper와 Gradle 설정의 actor 변수 이름이 완전히 통일되어 있지 않으므로, 로컬 Docker build 시에는 `GH_ACTOR`와 `GITHUB_ACTOR`를 같은 값으로 맞추는 편이 안전합니다.

## Data Bootstrap

- DB baseline DDL source: `db/schema.sql`
- EC2 배포용 schema copy: `deploy/ec2/db/schema.sql`
- 동기화 스크립트: `./scripts/sync-ec2-schema.sh`

환경별 MySQL bootstrap:

| Env | Config |
| --- | --- |
| `dev` | `docker/dev/services/mysql` |
| `prod` | `docker/prod/services/mysql` |

## CI

현재 CI는 아래 흐름을 가집니다.

1. contract lock 검증
2. test 실행
3. compose validate
4. bootJar build
5. Docker image build

기본 명령:

```bash
./gradlew --no-daemon clean test
./gradlew --no-daemon :app:bootJar -x test
```

## CD

현재 배포 프로필은 `ec2-compose`입니다.

- target: EC2
- image registry: ECR
- strategy: Docker Compose bundle deploy

주요 단계:

1. 환경 결정
2. AWS OIDC 인증
3. image build/push
4. contract source fetch
5. env/schema sync
6. remote compose deploy
7. health check와 smoke test

## Profile And Configuration

기본 설정은 `app/src/main/resources/application.yml`에 있고, profile별 세부값은 아래 import 구조로 분리됩니다.

- `application-<profile>_auth.yml`
- `application-<profile>_sso.yml`
- `application-<profile>_data.yml`
- `application-<profile>_db.yml`
- `application-<profile>_docs.yml`
- `application-<profile>_server.yml`

env 파일 규칙:

| Use Case | File |
| --- | --- |
| Local Java run | `.env.local` |
| Docker dev | `.env.dev` |
| Docker prod form validation | `.env.prod` |
| Template seed | `.env.example` |

주요 env 묶음:

- runtime: `SPRING_PROFILES_ACTIVE`, `SERVER_PORT`
- DB: `MYSQL_*`
- Redis: `REDIS_*`
- security: `AUTH_JWT_SECRET`, `INTERNAL_API_KEY`
- SSO: `SSO_GITHUB_*`, `SSO_*_ORIGIN`, `SSO_*_CALLBACK_URI`
- user-service: `USER_SERVICE_BASE_URL`, `USER_SERVICE_JWT_*`
- package access: `GH_ACTOR`, `GH_TOKEN`

profile 차이:

- dev: H2 fallback, Swagger enabled, admin IP guard 기본 비활성, cookie secure 완화
- prod: MySQL required, Swagger disabled, cookie `Secure`/`SameSite=None`, internal key 엄격 검증, Redis rate-limit backend

## Monitoring And Operations

Actuator 노출 범위:

- `/actuator/health`
- `/actuator/health/**`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/prometheus`

루트 상태 endpoint:

- `GET /` -> `{ "service": "auth-service", "status": "UP" }`

운영 관측 핵심:

- 기본 tag: `service`, `env`
- 요청 로그와 access log
- audit event: `AUTH_LOGIN_PASSWORD`, `AUTH_LOGIN_SSO`, `AUTH_LOGOUT`, `AUTH_INTERNAL_ACCOUNT_CREATE`, `AUTH_INTERNAL_ACCOUNT_DELETE`

우선 점검 포인트:

- `/actuator/health` 실패
- 로그인/refresh 실패율 증가
- Redis read/write 경고 증가
- OAuth callback/state 만료 경고 증가
- audit sink 장애 또는 prod startup fail-fast
