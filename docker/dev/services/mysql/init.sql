-- root 계정 외부 접속 권한 부여
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;

-- app 계정 및 DB 생성
CREATE DATABASE IF NOT EXISTS auth_service_db;
CREATE USER IF NOT EXISTS 'auth_user'@'%' IDENTIFIED BY 'auth_password';
GRANT ALL PRIVILEGES ON auth_service_db.* TO 'auth_user'@'%';
FLUSH PRIVILEGES;

-- 생성된 DB 사용
USE auth_service_db;

-- 테이블 schema는 repo 공통 baseline을 단일 source로 사용
SOURCE /schema/auth-schema.sql;
