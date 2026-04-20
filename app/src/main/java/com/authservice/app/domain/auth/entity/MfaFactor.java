package com.authservice.app.domain.auth.entity;

import com.authservice.common.base.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "mfa_factors")
@Getter
@NoArgsConstructor
public class MfaFactor extends BaseEntity {

	@Column(name = "user_id", nullable = false, length = 36, columnDefinition = "char(36)")
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID userId;

	@Column(name = "factor_type", nullable = false)
	private String factorType;

	@Column(name = "secret_ref", nullable = false)
	private String secretRef;

	@Column(name = "enabled", nullable = false)
	private boolean enabled;
}
