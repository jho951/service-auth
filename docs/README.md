# auth-service 문서

auth-service의 구현, 운영, 문제 해결 문서는 이 디렉토리에서 관리합니다.
서비스 간 책임, API 계약, 이벤트 계약, 공통 identity header 계약은 contract 레포를 기준으로 합니다.

## 문서 가이드

- [구조](./architecture.md): 모듈, 패키지, 의존 방향, 코드 배치 기준
- [auth-service API](./api.md): Gateway public route와 auth-service upstream route 계약
- [CI와 구현 기준](./ci-and-implementation.md): Gradle 멀티모듈, Java 17, CI, 로컬 구현 규칙
- [DB](./database.md): 관리 테이블, UUID `CHAR(36)` 바인딩, 신규 DB baseline
- [Docker](./docker.md): dev/prod Compose 구조, 네트워크, 실행 스크립트
- [Platform 사용 기준](./platform.md): `platform-security`, `platform-governance`, `platform-integrations` 소비 방식
- [문제 해결](./troubleshooting.md): 운영 판단 기준과 자주 막히는 문제
- [OpenAPI](./openapi/auth-service.yml): auth-service HTTP API 명세

## OpenAPI Sync

`docs/openapi/auth-service.yml`은 service-contract의 upstream OpenAPI local copy입니다. 계약이 바뀌면 contract repo의 auth-service upstream OpenAPI를 먼저 갱신한 뒤, 이 파일을 같은 내용으로 맞춥니다.
