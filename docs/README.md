# Auth-server 문서

Auth-server의 구현, 운영, 문제 해결 문서는 이 디렉토리에서 관리합니다.
서비스 간 책임, API 계약, 이벤트 계약, 공통 identity header 계약은 contract 레포를 기준으로 합니다.

## 문서 가이드

- [구조](./architecture.md): 모듈, 패키지, 의존 방향, 코드 배치 기준
- [Auth API](./auth-api.md): Gateway public route와 Auth-service upstream route 계약
- [CI와 구현 기준](./ci-and-implementation.md): Gradle 멀티모듈, Java 17, CI, 로컬 구현 규칙
- [DB](./database.md): 관리 테이블, UUID32 `CHAR(32)` 바인딩, 신규 DB baseline
- [Docker](./docker.md): dev/prod Compose 구조, 네트워크, 실행 스크립트
- [Platform 사용 기준](./platform.md): `platform-security`, `platform-governance`, `platform-integrations` 소비 방식
- [문제 해결](./troubleshooting.md): 운영 판단 기준과 자주 막히는 문제
- [OpenAPI](./openapi/auth-service.yml): auth-service HTTP API 명세

## OpenAPI Sync

`docs/openapi/auth-service.yml`은 service-contract의 upstream OpenAPI local copy입니다. 계약이 바뀌면 contract repo의 Auth upstream OpenAPI를 먼저 갱신한 뒤, 이 파일을 같은 내용으로 맞춥니다.

## 읽는 순서

1. 새 코드를 어디에 둘지 결정할 때는 [구조](./architecture.md)를 먼저 봅니다.
2. Auth HTTP 계약을 확인할 때는 [Auth API](./auth-api.md)를 봅니다.
3. 로컬 실행, Gradle, CI 기준이 필요하면 [CI와 구현 기준](./ci-and-implementation.md)를 봅니다.
4. 컨테이너 실행이나 네트워크를 바꿀 때는 [Docker](./docker.md)를 봅니다.
5. DB schema나 UUID32 바인딩을 바꿀 때는 [DB](./database.md)를 봅니다.
6. 인증 공통 capability나 감사/governance 연동을 바꿀 때는 [Platform 사용 기준](./platform.md)를 봅니다.
7. 운영 구조 판단이나 장애 대응 기준은 [문제 해결](./troubleshooting.md)에 남깁니다.
