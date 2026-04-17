CREATE DATABASE IF NOT EXISTS prod_db;
CREATE USER IF NOT EXISTS 'readonly_user'@'%' IDENTIFIED BY 'strongpassword';
GRANT SELECT ON `prod_db`.* TO 'readonly_user'@'%';
FLUSH PRIVILEGES;

-- 보안 관련 설정
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL slow_query_log_file = '/var/log/mysql/slow.log';

USE prod_db;

-- 테이블 schema는 repo 공통 baseline을 단일 source로 사용
SOURCE /schema/auth-schema.sql;
