package com.authservice.app.security.route;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/**
 * 보안 경계별 라우트 정책을 외부 구성 요소에 노출하는 파사드입니다.
 */
@Component
public class AuthRoutePolicy {

	/**
	 * 인증이 필요한 일반 보호 경로를 반환합니다.
	 *
	 * @return 보호 경로 패턴 배열
	 */
	public String[] protectedRequestMatchers() {
		return AuthRouteCatalog.patterns(AuthRouteCatalog.Boundary.PROTECTED, false);
	}

	/**
	 * 공개 경로 패턴을 반환합니다.
	 *
	 * @param includeDocs Swagger/OpenAPI 문서 경로 포함 여부
	 * @return 공개 경로 패턴 배열
	 */
	public String[] publicRequestMatchers(boolean includeDocs) {
		return AuthRouteCatalog.patterns(AuthRouteCatalog.Boundary.PUBLIC, includeDocs);
	}

	/**
	 * 내부 호출이 통과할 수 있는 경로 패턴을 반환합니다.
	 *
	 * @return 내부 경로 패턴 배열
	 */
	public String[] internalPassThroughRequestMatchers() {
		return AuthRouteCatalog.patterns(AuthRouteCatalog.Boundary.INTERNAL, false);
	}

	/**
	 * 관리자 보호 경로 패턴을 반환합니다.
	 *
	 * @return 관리자 경로 패턴 배열
	 */
	public String[] adminRequestMatchers() {
		return AuthRouteCatalog.patterns(AuthRouteCatalog.Boundary.ADMIN, false);
	}

	/**
	 * CSRF 검사를 건너뛸 매처 목록을 반환합니다.
	 *
	 * @return CSRF 무시 매처 배열
	 */
	public RequestMatcher[] csrfIgnoredMatchers() {
		return AuthRouteCatalog.csrfIgnoredMatchers();
	}

	/**
	 * 요청이 쿠키 기반 CSRF 원본 검증 대상인지 확인합니다.
	 *
	 * @param request 현재 HTTP 요청
	 * @return 쿠키 검증 대상 여부
	 */
	public boolean isCookieGuardedPath(HttpServletRequest request) {
		return AuthRouteCatalog.isCookieGuardedPath(request);
	}

	/**
	 * 요청이 내부 경계 경로인지 확인합니다.
	 *
	 * @param request 현재 HTTP 요청
	 * @return 내부 경계 경로 여부
	 */
	public boolean isInternalPath(HttpServletRequest request) {
		return AuthRouteCatalog.matchesBoundary(request, AuthRouteCatalog.Boundary.INTERNAL);
	}
}
