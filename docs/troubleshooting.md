# 문제 해결

## `db/schema.sql` 기준

이유:

- `auth-service`가 `auth_accounts`, `auth_login_attempts`, `mfa_factors`를 직접 소유합니다.
- `prod` profile은 `spring.jpa.hibernate.ddl-auto: none`입니다.
- 운영 DB schema는 애플리케이션 시작으로 자동 변경되지 않습니다.
- 따라서 신규 운영 DB를 처음 만들 baseline schema가 필요합니다.

현재 역할 분리는 아래와 같습니다.

```text
app/src/main/java/.../entity
  현재 애플리케이션의 DB 모델

app/src/main/resources/*_db.yml
  dev/prod DB 연결과 Hibernate DDL 정책

db/schema.sql
  신규 운영 DB baseline schema

scripts/db
  운영 SQL 실행 wrapper가 필요할 때 추가

docker/{dev,prod}/services/mysql/init.sql
  MySQL 컨테이너 최초 DB/user/권한 bootstrap
```

`db/schema.sql`은 테이블 DDL의 단일 source입니다. Docker `init.sql`은 DB/user/권한을 만든 뒤 `SOURCE /schema/auth-schema.sql;`로 `db/schema.sql`을 실행합니다. 현재 migration 파일은 두지 않습니다.

## MSA에서 DB schema 위치

MSA에서는 보통 서비스가 자기 데이터베이스 schema를 소유합니다.
따라서 `auth-service`가 소유하는 테이블의 baseline schema는 `auth-service` repo 안에 두는 것이 실무적으로 안전합니다.

좋은 PR 단위:

```text
entity 변경
repository/service 변경
db/schema.sql 변경
docs/database.md 변경
OpenAPI 변경이 있으면 docs/openapi 변경
```

피해야 할 방식:

```text
entity만 변경하고 db/schema.sql 미반영
운영 SQL을 개인 문서나 별도 위치에만 보관
Docker init.sql에 테이블 DDL 섞기
다른 서비스가 auth DB schema를 직접 변경
```

## Migration 미사용 기준

현재 baseline schema를 직접 관리하는 파일:

```text
db/schema.sql
docs/database.md
```

장점:

- 단순합니다.
- 현재 repo 구조와 잘 맞습니다.
- 신규 DB가 어떤 schema로 시작하는지 파일로 바로 확인할 수 있습니다.

한계:

- DB 내부에 schema 적용 이력이 자동으로 남지 않습니다.
- CI/CD 자동화 수준이 낮습니다.

현재 기준:

```text
지금은 db/schema.sql baseline 유지
현재 migration은 두지 않음
```

## UUID schema 기준

신규 DB는 처음부터 canonical UUID `CHAR(36)` 기준으로 생성합니다.

```sql
id CHAR(36) NOT NULL
```

따라서 `BINARY(16)`에서 `CHAR(36)`으로 바꾸려 했던 이유는 성능 최적화보다 운영 가독성, 디버깅 편의성, 서비스 간 UUID 표현의 일관성을 우선했기 때문입니다.

## AWS 배포에서 compute와 DB를 분리할지

auth-service를 AWS에 배포할 때 애플리케이션 compute와 MySQL runtime은 분리합니다. 현재 Terraform 기준은 ECS/Fargate Blue/Green과 RDS MySQL입니다.

MSA 운영 기준에서는 보통 아래 구조를 우선합니다.

```text
auth-service
  -> ECS/Fargate service
  -> ALB blue/green target group
  -> CodeDeploy traffic shift

auth DB
  -> RDS MySQL 또는 Aurora MySQL
```

핵심 기준은 `auth-service`가 auth DB schema를 소유하되, DB runtime은 애플리케이션 runtime과 분리하는 것입니다.
다른 서비스가 auth DB에 직접 접근하지 않는 원칙은 Docker MySQL 컨테이너를 쓰든 RDS를 쓰든 동일합니다.

RDS 분리를 우선하는 이유:

- compute 장애가 애플리케이션과 DB를 동시에 중단시키는 위험을 줄입니다.
- 백업, point-in-time recovery, patching, storage 증설을 AWS 관리 기능으로 처리할 수 있습니다.
- 애플리케이션 Blue/Green 배포와 DB lifecycle을 분리할 수 있습니다.
- Security Group으로 ECS task에서 오는 3306만 허용하기 쉽습니다.

MySQL 컨테이너를 compute와 함께 두는 방식은 아래 상황에서만 검토합니다.

- 개발 또는 임시 staging 환경입니다.
- 비용 절약이 장애 대응보다 우선인 초기 MVP입니다.
- 데이터 유실 위험, 백업, 복구, 디스크 증설을 직접 운영할 수 있습니다.
- 단일 서버 장애 시 app과 DB가 같이 내려가는 것을 받아들일 수 있습니다.

권장 배포 기준:

```text
1. ECS/Fargate + ALB + CodeDeploy Blue/Green
2. RDS MySQL
3. ElastiCache Redis
4. CloudWatch Logs / metrics
```

Terraform으로 구성할 때 기본 보안 경계:

```text
ALB Security Group
  inbound: HTTP/HTTPS 또는 제한된 client CIDR
  outbound: ECS task port

ECS Task Security Group
  inbound: ALB Security Group에서 오는 app port
  outbound: RDS 3306, Redis 6379, ECR, 외부 OAuth provider

RDS Security Group
  inbound: ECS Task Security Group에서 오는 3306만 허용
  public access: false
```

따라서 운영 배포 기본값은 `auth-service는 ECS/Fargate`, `MySQL은 RDS`로 둡니다. Docker Compose의 MySQL 컨테이너 구성은 로컬, 개발, 임시 검증 용도로 취급합니다.

## 빠른 판단 기준

```text
서비스가 테이블을 소유한다
  -> baseline schema는 서비스 repo에 둔다

prod ddl-auto가 none이다
  -> 신규 DB는 db/schema.sql로 만든다

Docker init.sql을 수정하려 한다
  -> DB/user bootstrap인지 먼저 확인하고, 테이블 DDL은 db/schema.sql에 둔다

DB 변경과 코드 변경이 다른 PR로 갈라진다
  -> 배포 순서와 rollback 위험을 다시 검토한다

AWS 운영 배포에서 MySQL 위치를 정한다
  -> 기본은 RDS 분리, MySQL 컨테이너는 개발/임시 환경으로 제한한다
```
