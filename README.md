# Auth-server

## 기준

- 프로젝트는 Gradle 멀티모듈로 실행 애플리케이션은 `app`, 내부 공통 코드는 `common`에 둡니다.
- Java 기준 버전은 17입니다.
- 인증 공통 기능은 `platform-security` `2.0.0` 기준으로 연동합니다.
- Docker Compose는 환경별로 `docker/dev/compose.yml`, `docker/prod/compose.yml`에서 관리합니다.
- Redis는 중앙 Redis를 사용합니다.
- DB 식별자는 uuid32를 기준으로 `CHAR(32)`에 저장합니다.
- 신규 DB 테이블 DDL은 `db/schema.sql`을 단일 기준으로 사용합니다.
- AWS 배포 기준은 `infra/terraform`의 ECS/Fargate Blue/Green + RDS 구조입니다.

## 빠른 시작

Private GitHub Packages 접근 권한이 필요합니다.

```bash
export GITHUB_ACTOR=your-github-id
export GITHUB_TOKEN=your-package-read-token
```

### Docker

```bash
./scripts/run.docker.sh up dev app
```

Swagger UI:

```text
http://localhost:8082/swagger-ui.html
```

### 로컬

```bash
cp docs/examples/env.local.example .env.local
./scripts/run.local.sh
```

IntelliJ로 실행할 때도 `.env.local` 값을 Run Configuration에 주입하고 active profile은 `dev`로 둡니다.

### 종료

```bash
./scripts/run.docker.sh down dev app
```

## [문서](./docs/README.md)

주요 문서:

- [구조](./docs/architecture.md)
- [Auth API](./docs/auth-api.md)
- [DB](./docs/database.md)
- [Docker](./docs/docker.md)
- [CI와 구현 기준](./docs/ci-and-implementation.md)
- [문제 해결](./docs/troubleshooting.md)
