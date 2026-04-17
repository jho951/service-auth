# Auth DB

이 문서는 DB 스키마를 변경할 때 확인해야 하는 구현 기준입니다.

## 테이블 종류

현재 애플리케이션이 직접 관리하는 테이블은 아래 3개입니다.

| 테이블 | 엔티티 | 용도 |
| --- | --- | --- |
| `auth_accounts` | `Auth` | 로그인 계정, password hash, 계정 잠금/실패 횟수 |
| `auth_login_attempts` | `AuthLoginAttempt` | 로그인 성공/실패 이력 |
| `mfa_factors` | `MfaFactor` | 사용자 MFA factor 메타데이터 |

`auth_audit_logs`는 이 서비스의 JPA entity가 관리하지 않습니다.
감사 로그는 `audit-log` 설정을 통해 파일 또는 외부 수집기로 보냅니다.

## UUID32 바인딩

DB에는 UUID를 하이픈 없는 32자리 문자열로 저장합니다.

```text
550e8400e29b41d4a716446655440000
```

구현 기준:

- DB type: `CHAR(32)`
- 저장 형식: lowercase UUID32, 하이픈 없음
- Java service/API type: `java.util.UUID`
- Entity 저장 필드: `String`
- 변환 기준: `Uuid32.fromUuid(UUID)`, `Uuid32.toUuid(String)`
- 신규 DB 기준 DDL: `db/schema.sql`

엔티티는 UUID 필드를 Hibernate UUID 타입에 맡기지 않습니다. DB 저장 필드는 `String`이고, public getter는 기존 서비스 코드와 API 호환을 위해 `UUID`를 반환합니다.

```java
@Column(name = "id", nullable = false, updatable = false, length = 32, columnDefinition = "char(32)")
private String id;

public UUID getId() {
	return Uuid32.toUuid(id);
}
```

## UUID 컬럼

| 테이블 | 컬럼 | Entity 저장 필드 | Public type | 비고 |
| --- | --- | --- | --- | --- |
| `auth_accounts` | `id` | `String Auth.id` | `UUID` | auth account primary key |
| `auth_accounts` | `user_id` | `String Auth.userId` | `UUID` | user-service user id, unique |
| `auth_login_attempts` | `id` | `String AuthLoginAttempt.id` | `UUID` | login attempt primary key |
| `mfa_factors` | `id` | `String MfaFactor.id` | `UUID` | MFA factor primary key |
| `mfa_factors` | `user_id` | `String MfaFactor.userId` | `UUID` | user-service user id |

새 UUID 컬럼을 추가할 때는 아래를 적용합니다.

```java
@Column(name = "new_uuid_column", nullable = false, length = 32, columnDefinition = "char(32)")
private String newUuidColumn;
```

외부 입력이 `UUID`이면 저장 전에 `Uuid32.fromUuid(value)`로 변환합니다. DB에서 읽은 값은 `Uuid32.toUuid(value)`로 외부에 반환합니다.

## 프로필별 DDL 정책

| 프로필 | DDL 정책 | 용도 |
| --- | --- | --- |
| `dev` | `spring.jpa.hibernate.ddl-auto: create` | 로컬 개발에서 entity 기준으로 스키마 재생성 |
| `prod` | `spring.jpa.hibernate.ddl-auto: none` | 운영 DB는 명시적 schema/migration으로만 변경 |

신규 운영 DB는 `db/schema.sql`을 기준으로 생성합니다. 과거 `BINARY(16)` 또는 `CHAR(36)`에서 변경하는 migration은 유지하지 않습니다.

`db/schema.sql`은 테이블 DDL의 단일 source입니다. Docker `init.sql`에는 테이블 생성 SQL을 복사하지 않고, DB/user bootstrap 후 `SOURCE /schema/auth-schema.sql;`로 이 파일을 실행합니다.

## 신규 DB 초기화

운영 DB를 처음 만들 때:

```bash
mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DB" \
  < db/schema.sql
```

Docker dev/prod 초기화도 같은 `db/schema.sql`을 mount해서 실행합니다.

Docker 실행 경로:

```text
docker/{dev,prod}/services/mysql/init.sql
  DB/user 생성, 권한 부여, USE <database>
  SOURCE /schema/auth-schema.sql

db/schema.sql
  auth_accounts, auth_login_attempts, mfa_factors 생성
```

## 스키마 확인

UUID 컬럼 타입 확인:

```sql
SELECT
	table_name,
	column_name,
	column_type,
	is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('auth_accounts', 'auth_login_attempts', 'mfa_factors')
  AND column_name IN ('id', 'user_id')
ORDER BY table_name, column_name;
```

기대 결과는 `column_type = char(32)`입니다.

UUID32 문자열 형식 검증:

```sql
SELECT id
FROM auth_accounts
WHERE id IS NOT NULL
  AND id NOT REGEXP '^[0-9a-f]{32}$'
LIMIT 20;
```

`user_id`도 같은 방식으로 확인합니다.

```sql
SELECT user_id
FROM auth_accounts
WHERE user_id IS NOT NULL
  AND user_id NOT REGEXP '^[0-9a-f]{32}$'
LIMIT 20;
```

결과가 없어야 정상입니다.

## Repository 계약

Repository의 DB ID type은 entity 저장 필드와 맞춰 `String`을 사용합니다.

```java
public interface AuthRepository extends JpaRepository<Auth, String> {
	Optional<Auth> findByUserId(String userId);

	default Optional<Auth> findByUserId(UUID userId) {
		return findByUserId(Uuid32.fromUuid(userId));
	}
}
```

서비스/API 계층은 기존처럼 `UUID`를 사용할 수 있습니다. DB 조회 전후 변환은 repository/entity 경계에서 처리합니다.

## 변경 규칙

DB 변경 시 지켜야 할 기준:

- 신규 DB baseline은 `db/schema.sql`에만 반영합니다.
- Docker `init.sql`에는 DB/user/권한 bootstrap만 둡니다.
- 운영 중인 테이블 변경이 필요할 때만 migration SQL을 추가합니다.
- UUID 컬럼은 entity와 SQL 양쪽에 `CHAR(32)`을 명시합니다.
- UUID32 저장/복원은 `Uuid32` 유틸을 사용합니다.
- `user_id`는 user-service의 사용자 ID이므로 auth-service에서 값을 새로 생성하지 않습니다.
- 새 public API가 UUID를 주고받으면 `docs/openapi/auth-service.yml`도 같이 갱신합니다.
