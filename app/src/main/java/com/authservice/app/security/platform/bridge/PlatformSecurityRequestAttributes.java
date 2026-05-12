package com.authservice.app.security.platform.bridge;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * platform-security 브리지 계층에서 사용하는 요청 attribute 키 모음입니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlatformSecurityRequestAttributes {

	/** 브리지된 액세스 토큰 attribute 키 */
	public static final String ACCESS_TOKEN_ATTRIBUTE = "auth.accessToken";
	/** 브리지된 세션 식별자 attribute 키 */
	public static final String SESSION_ID_ATTRIBUTE = "auth.sessionId";
	/** 브리지된 내부 호출 증명 토큰 attribute 키 */
	public static final String INTERNAL_TOKEN_ATTRIBUTE = "auth.internalToken";
	/** 판별된 플랫폼 보안 경계 attribute 키 */
	public static final String SECURITY_BOUNDARY_ATTRIBUTE = "security.boundary";
}
