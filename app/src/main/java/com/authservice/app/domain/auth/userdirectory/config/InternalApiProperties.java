package com.authservice.app.domain.auth.userdirectory.config;

import com.authservice.app.common.base.constant.ErrorCode;
import com.authservice.app.common.base.exception.GlobalException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "internal.api")
public class InternalApiProperties {

	private String key;

	public static final String INTERNAL_SECRET_HEADER = "X-Internal-Request-Secret";

	public void validateInternalAccess(String authorization, String internalRequestSecret) {
		if (isValidAuthorizationHeader(authorization) || isValidInternalSecret(internalRequestSecret)) return;
		throw new GlobalException(ErrorCode.UNAUTHORIZED);
	}

	private boolean isValidAuthorizationHeader(String authorization) {
		String expected = "Bearer " + key;
		return key != null && !key.isBlank() && expected.equals(authorization);
	}

	private boolean isValidInternalSecret(String internalRequestSecret) {
		return key != null && !key.isBlank() && key.equals(internalRequestSecret);
	}
}
