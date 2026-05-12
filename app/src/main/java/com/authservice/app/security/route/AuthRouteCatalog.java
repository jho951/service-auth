package com.authservice.app.security.route;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * auth-service가 사용하는 보안 경계별 라우트 정의를 보관하는 내부 카탈로그입니다.
 */
final class AuthRouteCatalog {

	/** 플랫폼 보안 경계 분류입니다. */
	enum Boundary {
		PUBLIC,
		PROTECTED,
		INTERNAL,
		ADMIN
	}

	private static final List<RouteSpec> ROUTES = List.of(
		RouteSpec.publicRoute("/"),
		RouteSpec.publicRoute("/.well-known/**"),
		RouteSpec.publicRoute("/error"),
		RouteSpec.publicRoute("/health"),
		RouteSpec.publicRoute("/actuator/prometheus"),
		RouteSpec.publicRoute("/actuator/health"),
		RouteSpec.publicRoute("/actuator/health/**"),
		RouteSpec.publicRoute("/favicon.ico"),
		RouteSpec.publicRoute("/auth/login"),
		RouteSpec.publicRoute("/auth/refresh"),
		RouteSpec.publicRoute("/auth/logout"),
		RouteSpec.publicRoute("/auth/sso/start"),
		RouteSpec.publicRoute("/auth/session"),
		RouteSpec.publicRoute("/auth/me"),
		RouteSpec.publicRoute("/auth/oauth/github/callback"),
		RouteSpec.publicRoute("/auth/login/github"),
		RouteSpec.publicRoute("/auth/oauth2/authorize/github"),
		RouteSpec.publicRoute("/auth/exchange"),
		RouteSpec.publicRoute("/oauth2/**"),
		RouteSpec.publicRoute("/login/oauth2/**"),
		RouteSpec.protectedRoute("/api/**"),
		RouteSpec.adminRoute("/admin/**"),
		RouteSpec.internalRoute("/internal/**"),
		RouteSpec.docsRoute("/v3/api-docs/**"),
		RouteSpec.docsRoute("/swagger-ui/**"),
		RouteSpec.docsRoute("/swagger-ui.html")
	);

	private static final List<MethodRouteSpec> CSRF_IGNORED_ROUTES = List.of(
		MethodRouteSpec.any("/internal/auth/**"),
		MethodRouteSpec.post("/auth/login"),
		MethodRouteSpec.post("/auth/refresh"),
		MethodRouteSpec.post("/auth/logout"),
		MethodRouteSpec.post("/internal/auth/session/validate"),
		MethodRouteSpec.post("/auth/exchange")
	);

	private static final Set<MethodRouteSpec> COOKIE_GUARDED_POST_ROUTES = Set.of(
		MethodRouteSpec.post("/auth/refresh"),
		MethodRouteSpec.post("/auth/logout")
	);

	private AuthRouteCatalog() {
	}

	/**
	 * 지정한 보안 경계에 해당하는 경로 패턴 목록을 반환합니다.
	 *
	 * @param boundary 조회할 보안 경계
	 * @param includeDocs 문서 경로 포함 여부
	 * @return 경계별 요청 경로 패턴 배열
	 */
	static String[] patterns(Boundary boundary, boolean includeDocs) {
		List<String> patterns = new ArrayList<>();
		for (RouteSpec route : ROUTES) {
			if (route.boundary() != boundary) {
				continue;
			}
			if (route.docsOnly() && !includeDocs) {
				continue;
			}
			patterns.add(route.pattern());
		}
		return patterns.toArray(String[]::new);
	}

	/**
	 * CSRF 예외 처리 대상 라우트를 매처로 변환합니다.
	 *
	 * @return CSRF 무시 대상 매처 배열
	 */
	static RequestMatcher[] csrfIgnoredMatchers() {
		PathPatternRequestMatcher.Builder pathMatcher = PathPatternRequestMatcher.withDefaults();
		return CSRF_IGNORED_ROUTES.stream()
			.map(route -> route.toMatcher(pathMatcher))
			.toArray(RequestMatcher[]::new);
	}

	/**
	 * 요청이 지정한 보안 경계에 속하는지 확인합니다.
	 *
	 * @param request 현재 HTTP 요청
	 * @param boundary 판별할 보안 경계
	 * @return 보안 경계 일치 여부
	 */
	static boolean matchesBoundary(HttpServletRequest request, Boundary boundary) {
		String path = normalizePath(request);
		for (String pattern : patterns(boundary, false)) {
			if (matches(path, pattern)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 요청이 쿠키 기반 CSRF 원본 검증 대상인지 판단합니다.
	 *
	 * @param request 현재 HTTP 요청
	 * @return 쿠키 기반 보호 대상 여부
	 */
	static boolean isCookieGuardedPath(HttpServletRequest request) {
		return matchesMethodRoute(request, COOKIE_GUARDED_POST_ROUTES);
	}

	private static boolean matchesMethodRoute(HttpServletRequest request, Set<MethodRouteSpec> routes) {
		String method = request.getMethod();
		String path = normalizePath(request);
		for (MethodRouteSpec route : routes) {
			if (route.matches(method, path)) {
				return true;
			}
		}
		return false;
	}

	private static String normalizePath(HttpServletRequest request) {
		String path = request.getRequestURI();
		String contextPath = request.getContextPath();
		if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
			return path.substring(contextPath.length());
		}
		return path;
	}

	private static boolean matches(String path, String pattern) {
		if (path == null || pattern == null) {
			return false;
		}
		if (pattern.endsWith("/**")) {
			String basePath = pattern.substring(0, pattern.length() - 3);
			return path.equals(basePath) || path.startsWith(basePath + "/");
		}
		return path.equals(pattern);
	}

	private record RouteSpec(String pattern, Boundary boundary, boolean docsOnly) {

		private static RouteSpec publicRoute(String pattern) {
			return new RouteSpec(pattern, Boundary.PUBLIC, false);
		}

		private static RouteSpec protectedRoute(String pattern) {
			return new RouteSpec(pattern, Boundary.PROTECTED, false);
		}

		private static RouteSpec internalRoute(String pattern) {
			return new RouteSpec(pattern, Boundary.INTERNAL, false);
		}

		private static RouteSpec adminRoute(String pattern) {
			return new RouteSpec(pattern, Boundary.ADMIN, false);
		}

		private static RouteSpec docsRoute(String pattern) {
			return new RouteSpec(pattern, Boundary.PUBLIC, true);
		}
	}

	private record MethodRouteSpec(HttpMethod method, String pattern) {

		private static MethodRouteSpec any(String pattern) {
			return new MethodRouteSpec(null, pattern);
		}

		private static MethodRouteSpec post(String pattern) {
			return new MethodRouteSpec(HttpMethod.POST, pattern);
		}

		private RequestMatcher toMatcher(PathPatternRequestMatcher.Builder pathMatcher) {
			return method == null ? pathMatcher.matcher(pattern) : pathMatcher.matcher(method, pattern);
		}

		private boolean matches(String candidateMethod, String candidatePath) {
			if (method != null && !method.name().equalsIgnoreCase(candidateMethod)) {
				return false;
			}
			return AuthRouteCatalog.matches(candidatePath, pattern);
		}
	}
}
