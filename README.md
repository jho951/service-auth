# auth-service

## 기준

- 프로젝트는 Gradle 멀티모듈로 실행 애플리케이션은 `app`, 내부 공통 코드는 `common`에 둡니다.
- Java 기준 버전은 17입니다.
- 인증 공통 기능은 `platform-runtime-bom` `3.0.1`과 `platform-security` `3.0.1` 기준으로 연동합니다.
- Docker Compose는 환경별로 `docker/dev/compose.yml`, `docker/prod/compose.yml`에서 관리합니다.
- Redis는 중앙 Redis를 사용합니다.
- DB 식별자는 canonical UUID 문자열 기준으로 `CHAR(36)`에 저장합니다.
- 신규 DB 테이블 DDL은 `db/schema.sql`을 단일 기준으로 사용합니다.
- AWS 배포 기준은 `infra/terraform`의 ECS/Fargate Blue/Green + RDS 구조입니다.

## Contract Source

- 공통 계약 레포: `https://github.com/jho951/service-contract`
- 계약 동기화 기준 파일: [contract.lock.yml](contract.lock.yml)
- 계약 변경 절차: [contract-change-workflow.md](docs/contract-change-workflow.md)
- PR에서는 `.github/workflows/contract-check.yml`이 lock 파일과 계약 영향 변경 여부를 검사합니다.
- 인터페이스 변경 시 본 저장소 구현보다 계약 레포 변경을 먼저 반영합니다.

## 빠른 시작

Private GitHub Packages 접근 권한이 필요합니다.

```bash
export GITHUB_ACTOR=your-github-id
export GH_TOKEN=your-package-read-token
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

루트에 `.env.local`을 만들고 `./scripts/run.local.sh`를 실행합니다.
IntelliJ로 실행할 때도 `.env.local` 값을 Run Configuration에 주입하고 active profile은 `dev`로 둡니다.

### 종료

```bash
./scripts/run.docker.sh down dev app
```

## [문서](./docs/README.md)
