-- Auth-service baseline schema.
-- UUID identifiers are stored as UUID32: 32 lowercase hexadecimal characters without hyphens.

CREATE TABLE IF NOT EXISTS auth_accounts (
	id CHAR(32) NOT NULL,
	user_id CHAR(32) NOT NULL,
	login_id VARCHAR(255) NOT NULL,
	password_hash VARCHAR(255) NOT NULL,
	account_locked BIT NOT NULL,
	failed_login_count INT NOT NULL,
	password_updated_at DATETIME(6) NOT NULL,
	last_login_at DATETIME(6) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	PRIMARY KEY (id),
	UNIQUE KEY uk_auth_accounts_user_id (user_id),
	UNIQUE KEY uk_auth_accounts_login_id (login_id)
);

CREATE TABLE IF NOT EXISTS auth_login_attempts (
	id CHAR(32) NOT NULL,
	login_id VARCHAR(255) NOT NULL,
	ip VARCHAR(255) NULL,
	result VARCHAR(255) NOT NULL,
	attempted_at DATETIME(6) NOT NULL,
	PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS mfa_factors (
	id CHAR(32) NOT NULL,
	user_id CHAR(32) NOT NULL,
	factor_type VARCHAR(255) NOT NULL,
	secret_ref VARCHAR(255) NOT NULL,
	enabled BIT NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	PRIMARY KEY (id),
	KEY ix_mfa_factors_user_id (user_id)
);
