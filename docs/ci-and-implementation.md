# CI와 구현 기준

## Gradle 프로젝트

Gradle 루트는 멀티모듈 집계 프로젝트입니다.

```text
auth-service
├── app
└── common
```

모듈 구성:

| 모듈 | Gradle 플러그인 | 로컬 역할 |
| --- | --- | --- |
| root | `base` | 전체 모듈 집계, 공통 group/version 관리 |
| `app` | Spring Boot | 실행 가능한 auth-service 애플리케이션 |
| `common` | `java-library` | 공통 응답/예외/로깅/Redis/Swagger 인프라 |

루트 `settings.gradle`는 아래 모듈만 include합니다.

```groovy
include 'app'
include 'common'
```

## Java와 테스트 기준

공통 기준:

- Java toolchain: 17
- 테스트 플랫폼: JUnit Platform
- 의존성 버전 카탈로그: `gradle/libs.versions.toml`

루트 `build.gradle`의 `subprojects` 블록이 Java toolchain과 test platform을 공통 적용합니다.

```groovy
subprojects {
    apply plugin: 'java'

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(providers.gradleProperty('java_version').get().toInteger())
        }
    }

    tasks.withType(Test).configureEach {
        useJUnitPlatform()
    }
}
```

## 의존성 해석

Repository는 `settings.gradle`의 `dependencyResolutionManagement`에서 중앙 관리합니다.

사용 repository:

- Maven Central
- GitHub Packages: `https://maven.pkg.github.com/jho951/platform-governance`
- GitHub Packages: `https://maven.pkg.github.com/jho951/platform-security`
- GitHub Packages: `https://maven.pkg.github.com/jho951/platform-integrations`

`platform-governance`, `platform-security`, `platform-integrations`는 private package이므로 인증값이 필요합니다.
현재 기준 버전은 `platform-governance` `2.0.1`이며, `gradle/libs.versions.toml`에서 중앙 관리합니다.
현재 기준 버전은 `platform-security` `2.0.3`이며, `gradle/libs.versions.toml`에서 중앙 관리합니다.
현재 기준 버전은 `platform-integrations` `1.0.1`이며, `gradle/libs.versions.toml`에서 중앙 관리합니다.

로컬 shell:

```bash
export GITHUB_ACTOR=your-github-id
export GH_TOKEN=your-package-read-token
```

또는 `~/.gradle/gradle.properties`:

```properties
githubPackagesUsername=your-github-id
githubPackagesToken=your-package-read-token
```

기존 로컬 설정과의 호환을 위해 `githubUsername`, `githubToken`도 fallback으로 읽습니다.

CI:

```yaml
env:
  GITHUB_ACTOR: ${{ github.actor }}
  GH_TOKEN: ${{ secrets.GH_TOKEN || github.token }}
```

`GH_TOKEN`에는 `platform-governance`, `platform-security`, `platform-integrations` package read 권한이 필요합니다.

## CI workflow

현재 GitHub Actions workflow:

| workflow | 파일 | 실행 조건 | 주요 명령 |
| --- | --- | --- | --- |
| CI | `.github/workflows/ci.yml` | branch push/PR | `./gradlew clean build`, Docker Compose config, Docker image build |
| CD | `.github/workflows/cd.yml` | `main` push, `v*` tag, manual dispatch | test, ECR image push, ECS task definition registration, CodeDeploy deployment |
| contract-check | `.github/workflows/contract-check.yml` | PR | contract impact check |
| CodeQL | GitHub default setup | repository security setting | CodeQL analyze |

CI 공통 기준:

- Runner: `ubuntu-latest`
- JDK: Temurin 17
- package 권한: `packages: read`
- source 권한: `contents: read`

CodeQL은 GitHub default setup을 사용합니다. Repository default setup과 advanced workflow를 동시에 켜면 SARIF 처리 충돌이 발생하므로 `.github/workflows/codeql.yml`을 별도로 두지 않습니다.

## CD workflow

CD는 Terraform이 만든 ECS/Fargate Blue/Green 인프라에 새 이미지를 배포합니다.

흐름:

1. `./gradlew --no-daemon clean test`
2. Docker image build
3. Amazon ECR push
4. 현재 ECS service task definition 조회
5. container image만 새 ECR image URI로 바꾼 task definition revision 등록
6. CodeDeploy ECS deployment 생성
7. 기본값으로 deployment 성공까지 wait

GitHub secret:

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `AWS_ROLE_ARN` | yes | GitHub OIDC가 assume할 AWS IAM role ARN |
| `GH_TOKEN` | yes | Docker build 중 private GitHub Packages read 권한 |

GitHub repository/environment vars:

| 이름 | 기본값 | 설명 |
| --- | --- | --- |
| `AWS_REGION` | `ap-northeast-2` | AWS region |
| `ECR_REPOSITORY_NAME` | `prod-auth-service` | Terraform ECR repository name |
| `ECS_CLUSTER_NAME` | `prod-auth-service-cluster` | ECS cluster name |
| `ECS_SERVICE_NAME` | `prod-auth-service` | ECS service name |
| `ECS_CONTAINER_NAME` | `auth-service` | ECS task container name |
| `ECS_CONTAINER_PORT` | `8081` | AppSpec container port |
| `CODEDEPLOY_APPLICATION_NAME` | `prod-auth-service-codedeploy` | CodeDeploy application |
| `CODEDEPLOY_DEPLOYMENT_GROUP_NAME` | `prod-auth-service-blue-green` | CodeDeploy deployment group |

기본값은 `infra/terraform`의 기본 `environment=prod`, `service_name=auth-service`, `service_runtime_name=auth-service` 조합과 맞춥니다. Terraform 값을 바꾸면 GitHub vars도 같이 바꿉니다.

## 로컬 검증

일반 검증:

```bash
./gradlew build
./gradlew test
```

빠른 컴파일 확인:

```bash
./gradlew :app:compileJava :common:compileJava
```

로컬 실행:

```bash
./scripts/run.local.sh
```

`run.local.sh`는 기본적으로 루트 `.env.local`을 읽고 Spring `dev` profile로 실행합니다. `run.docker.sh`는 환경별 루트 `.env.{env}` 파일을 요구합니다. Docker 실행 기준은 [docker.md](./docker.md)에 정리되어 있습니다.

Docker image build는 Dockerfile 내부에서 아래를 실행합니다.

```bash
./gradlew :app:build --no-daemon -x test
```

Docker image의 기본 `SPRING_PROFILES_ACTIVE`는 `dev`입니다. ECS 배포에서는 Terraform task definition의 `SPRING_PROFILES_ACTIVE` 환경 변수가 이 값을 덮어써야 하며, prod 기본값은 `prod`입니다.

## 구현 규칙

모듈 의존 방향:

- `app`은 `common`에 의존할 수 있습니다.
- `common`은 `app`에 의존하면 안 됩니다.
- 외부 서비스별 client boundary는 `app` 도메인 하위 패키지에 둡니다.
- 공통 응답/예외/로깅/인프라만 `common`으로 올립니다.

Spring Boot 실행 진입점:

- `app/src/main/java/com/authservice/app/AuthServiceApplication.java`

리소스 구성:

- 공통 설정: `app/src/main/resources/application.yml`
- dev profile: `app/src/main/resources/dev`
- prod profile: `app/src/main/resources/prod`

구현 시 코드 배치 기준은 [architecture.md](./architecture.md)를 따릅니다.

DB 변경 기준은 [database.md](./database.md)를 따릅니다.

Docker 실행 기준은 [docker.md](./docker.md)를 따릅니다.

## 변경 체크리스트

코드 변경 전후 확인:

1. 새 코드가 `app`인지 `common`인지 먼저 결정합니다.
2. 새 dependency는 `gradle/libs.versions.toml` 관리가 필요한지 확인합니다.
3. private dependency가 추가되면 CI secret과 Docker build arg 영향도 확인합니다.
4. public API 변경이면 `docs/openapi/auth-service.yml`을 같이 갱신합니다.
5. DB 변경이면 `db/schema.sql` 기준이 코드와 일치하는지 먼저 확인합니다.
6. Docker runtime 값이 바뀌면 `.env.dev`, `.env.prod`, `docker/{dev,prod}/compose.yml`, [docker.md](./docker.md)를 같이 갱신합니다.
7. 최소 `./gradlew test` 또는 변경 범위에 맞는 Gradle task를 실행합니다.
