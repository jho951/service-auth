# Auth-server 구조

## 계약 기준

- 공통 계약 레포: `https://github.com/jho951/contract`
- 보안 플랫폼 레포: `https://github.com/jho951/platform-security`
- 이 서비스의 구현 기준 브랜치: `Auth-server` `main`
- 서비스 간 책임, API 계약, 이벤트 계약, 공통 identity header 계약은 contract 레포를 기준으로 합니다.
- 이 문서는 contract를 반복 정의하지 않고, Auth-server 내부 구현 배치 기준만 다룹니다.
- 인터페이스 변경 시 본 저장소 구현보다 계약 레포 변경을 먼저 반영합니다.

## 모듈 경계

| 모듈          | 로컬 역할                           | 둘 위치                                                      | 두지 않을 것                            |
|-------------|---------------------------------|-----------------------------------------------------------|------------------------------------|
| `app`       | 실행 애플리케이션과 auth-service 업무 로직   | 인증, SSO, 세션, 토큰, 원격 user-service 연동, Spring Security 조립   | 여러 서비스가 공유해야 하는 범용 응답/예외/인프라       |
| `common`    | 서비스 내부 공통 인프라                   | 공통 응답 모델, 공통 예외 처리, Redis/Swagger/로깅 설정                   | auth-service 도메인 규칙, 외부 서비스별 클라이언트 |
| `db`        | DB baseline schema                  | 신규 DB 테이블 DDL, 필요 시 향후 운영 migration                    | 애플리케이션 코드                          |
| `docker`    | 컨테이너 실행 정의                      | auth-service Compose, Dockerfile, DB seed/config          | 로컬 shell orchestration             |
| `scripts`   | 로컬/운영 보조 명령                     | Docker 실행 래퍼, 필요 시 DB 적용 스크립트                           | 서비스 런타임 로직                         |
| `docs`      | 설계와 운영 문서                       | 구조, DB, 보안 플랫폼, OpenAPI                                   | 코드가 기준이어야 하는 구현 세부사항               |

## 패키지 경계

`app/src/main/java/com/authservice/app`

| 패키지 | 로컬 역할 |
| --- | --- |
| `common.web` | 서비스 공통 HTTP endpoint와 servlet filter |
| `domain.auth` | 이메일/비밀번호 인증, refresh rotation, auth account persistence |
| `domain.auth.internal` | 내부 서비스 간 인증 API |
| `domain.auth.sso` | OAuth/SSO 시작, callback, ticket exchange, SSO cookie/session |
| `domain.auth.userdirectory` | user-service 원격 사용자 디렉토리 연동 |
| `domain.auth.support` | auth 도메인 내부에서 재사용하는 HTTP/cookie/token/uuid32 helper |
| `domain.audit` | auth-service 감사 이벤트 기록 어댑터 |
| `security` | Spring Security와 `platform-security` 연결 |

`common/src/main/java/com/authservice/app/common`

| 패키지 | 로컬 역할 |
| --- | --- |
| `base` | 공통 응답, 공통 예외, base entity |
| `logging` | 요청 MDC, access log, 민감정보 마스킹 |
| `redis` | Redis connection 설정 |
| `swagger` | OpenAPI/Swagger 설정 |

## 의존 방향

- `app`은 `common`에 의존할 수 있습니다.
- `common`은 `app`에 의존하면 안 됩니다.
- 도메인 패키지는 controller 패키지에 의존하지 않습니다.
- Controller는 request/response 변환과 흐름 위임만 담당하고, 판단은 service에 위임합니다.
- `domain.auth.userdirectory`가 user-service client 경계를 소유합니다. 다른 auth 패키지는 새 user-service client를 만들지 말고 `UserDirectory`를 호출합니다.
- `security`는 filter, token service, `platform-security` 연결을 담당합니다. 도메인 service는 Spring Security 설정 클래스를 import하지 않습니다.
- Auth-server는 provider OAuth2 login flow를 도메인 계층에 두고, identity 검증 이후 platform bridge/capability 계약을 사용합니다.

## 코드 배치 규칙

- 새 auth API request/response DTO는 `domain.auth.dto` 또는 더 좁은 하위 도메인 DTO 패키지에 둡니다.
- SSO 전용 state, model, redirect 개념은 `domain.auth.sso`에 둡니다.
- user-service 프로필 조회는 `domain.auth.userdirectory.service.UserDirectory` 뒤에 둡니다.
- cookie나 refresh token 추출 helper가 auth 전용이면 `domain.auth.support`에 둡니다.
- 여러 도메인에서 재사용할 응답/예외/로깅 코드는 `common`에 둡니다.
- 환경별 런타임 값은 `app/src/main/resources/{dev,prod}` 또는 루트 `.env.dev`/`.env.prod`에 둡니다.
- 신규 DB baseline 변경은 `db/schema.sql`에 반영합니다. 운영 중인 테이블 변경이 필요해질 때만 `db/migrations`를 추가합니다.

## 테스트 배치

- 단일 컴포넌트를 검증하는 단위 테스트는 production package를 따라 배치합니다.
- 여러 패키지를 조합하는 흐름 테스트는 `com.authservice.app` 아래에 둘 수 있습니다.
- Redis, DB, Docker 의존 테스트는 integration test로 이름을 구분하고 Gradle/JUnit 설정에서 선택 실행되게 둡니다.

## 변경 체크리스트

새 dependency 또는 패키지를 추가하기 전에 확인합니다.

1. 코드가 `app`에 속하는지 `common`에 속하는지 먼저 결정합니다.
2. 이미 해당 책임을 가진 패키지 경계가 있는지 확인합니다.
3. 외부 서비스 호출은 경계별 interface 뒤에 둡니다.
4. 변경 패키지 가까이에 집중된 테스트를 추가하거나 갱신합니다.
5. public contract가 바뀌면 OpenAPI 또는 문서를 같이 갱신합니다.
