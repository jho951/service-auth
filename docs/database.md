# auth-service DB

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

## UUID 바인딩

DB에는 UUID를 하이픈이 포함된 canonical 36자리 문자열로 저장합니다.

```text
550e8400-e29b-41d4-a716-446655440000
```

구현 기준:

- DB type: `CHAR(36)`
- 저장 형식: canonical UUID string, 하이픈 포함
- Java service/API type: `java.util.UUID`
- Entity 저장 필드: `UUID`
- Hibernate binding: `@JdbcTypeCode(SqlTypes.CHAR)`
- 신규 DB 기준 DDL: `db/schema.sql`

공통 entity ID는 `BaseEntity`가 소유합니다.

```java
@Id
@GeneratedValue(strategy = GenerationType.UUID)
@Column(nullable = false, updatable = false, length = 36, columnDefinition = "char(36)")
@JdbcTypeCode(SqlTypes.CHAR)
private UUID id;
```

도메인 소유 UUID 컬럼도 같은 저장 형식을 사용합니다.

```java
@Column(name = "user_id", nullable = false, length = 36, columnDefinition = "char(36)")
@JdbcTypeCode(SqlTypes.CHAR)
private UUID userId;
```

## UUID 컬럼

| 테이블 | 컬럼 | Entity 저장 필드 | Public type | 비고 |
| --- | --- | --- | --- | --- |
| `auth_accounts` | `id` | `BaseEntity.id` | `UUID` | auth account primary key |
| `auth_accounts` | `user_id` | `UUID Auth.userId` | `UUID` | user-service user id, unique |
| `auth_login_attempts` | `id` | `String AuthLoginAttempt.id` | `UUID` | login attempt primary key |
| `mfa_factors` | `id` | `BaseEntity.id` | `UUID` | MFA factor primary key |
| `mfa_factors` | `user_id` | `UUID MfaFactor.userId` | `UUID` | user-service user id |

`AuthLoginAttempt`는 별도 entity base를 쓰지 않지만, ID 문자열은 canonical UUID `CHAR(36)`만 사용합니다.

새 UUID 컬럼을 추가할 때는 아래를 적용합니다.

```java
@Column(name = "new_uuid_column", nullable = false, length = 36, columnDefinition = "char(36)")
@JdbcTypeCode(SqlTypes.CHAR)
private UUID newUuidColumn;
```

## 프로필별 DDL 정책

| 프로필 | DDL 정책 | 용도 |
| --- | --- | --- |
| `dev` | `spring.jpa.hibernate.ddl-auto: create` | 로컬 개발에서 entity 기준으로 스키마 재생성 |
| `prod` | `spring.jpa.hibernate.ddl-auto: none` | 운영 DB는 `db/schema.sql` 기준으로 직접 생성 |

신규 운영 DB는 `db/schema.sql`을 기준으로 생성합니다.
현재는 migration SQL을 두지 않습니다.

`db/schema.sql`은 테이블 DDL의 단일 source입니다.
Docker `init.sql`에는 테이블 생성 SQL을 복사하지 않고, DB/user bootstrap 후 `SOURCE /schema/auth-schema.sql;`로 이 파일을 실행합니다.

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

기대 결과는 `column_type = char(36)`입니다.

canonical UUID 문자열 형식 검증:

```sql
SELECT id
FROM auth_accounts
WHERE id IS NOT NULL
  AND id NOT REGEXP '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
LIMIT 20;
```

`user_id`도 같은 방식으로 확인합니다.

```sql
SELECT user_id
FROM auth_accounts
WHERE user_id IS NOT NULL
  AND user_id NOT REGEXP '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
LIMIT 20;
```

결과가 없어야 정상입니다.

## Repository 계약

Repository의 DB ID type은 entity ID type과 맞춰 `UUID`를 사용합니다.

```java
public interface AuthRepository extends JpaRepository<Auth, UUID> {
	Optional<Auth> findByUserId(UUID userId);
	Optional<Auth> findByLoginId(String loginId);
}
```

서비스/API 계층도 `UUID`를 그대로 사용합니다.

## 변경 규칙

DB 변경 시 지켜야 할 기준:

- 신규 DB baseline은 `db/schema.sql`에만 반영합니다.
- Docker `init.sql`에는 DB/user/권한 bootstrap만 둡니다.
- 현재 migration SQL은 두지 않습니다.
- UUID 컬럼은 entity와 SQL 양쪽에 `CHAR(36)`을 명시합니다.
- UUID 저장 형식은 canonical string만 사용합니다.
- `user_id`는 user-service의 사용자 ID이므로 auth-service에서 값을 새로 생성하지 않습니다.
- 새 public API가 UUID를 주고받으면 `docs/openapi/auth-service.yml`도 같이 갱신합니다.
