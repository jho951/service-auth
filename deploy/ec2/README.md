# EC2 Deploy Assets

이 디렉터리는 EC2에 앱 소스를 clone 하지 않고, 배포용 파일만 올리는 운영 방식을 위한 최소 산출물입니다.
현재 운영 기준 문서는 [docs/deployment.md](../../docs/deployment.md)입니다.

## 서버에 두는 파일

- `docker-compose.yml`
- `.env.production`
- `services/mysql/my.cnf`
- `services/mysql/init.sql`
- `db/schema.sql`

`db/schema.sql`은 이 디렉터리의 원본이 아닙니다.
repo 루트 `db/schema.sql`을 `./scripts/sync-ec2-schema.sh`로 동기화한 배포 번들용 복사본입니다.

repo에서 관리하는 생성/원본 파일:

- `deploy.env.example`
- `.env.production.example`

`deploy.env.example`은 EC2 번들 전용 값만 담는 source file입니다.
`.env.production.example`은 루트 `.env.prod`와 `deploy.env.example`을 `./scripts/sync-ec2-env-template.sh`로 합쳐 만든 생성 산출물입니다.

## 운영 원칙

- EC2에는 `auth-service` 앱 소스를 clone 하지 않습니다.
- EC2는 Docker image를 pull 받아 실행하는 서버입니다.
- 서비스는 기본적으로 `127.0.0.1:8081` 로만 바인딩합니다.
- 이 디렉터리는 repo 기준 Compose가 아니라 서버 복사용 standalone bundle입니다.

## 배포 순서

1. repo에서 `./scripts/sync-ec2-env-template.sh`를 실행해 `.env.production.example`을 최신화합니다.
2. EC2에 이 디렉터리의 파일만 복사합니다.
3. `.env.production.example` 을 `.env.production` 으로 복사하고 환경별 값을 검토/수정합니다.
4. 아래 명령으로 이미지를 갱신합니다.

```bash
docker compose --env-file .env.production pull
docker compose --env-file .env.production up -d
```
