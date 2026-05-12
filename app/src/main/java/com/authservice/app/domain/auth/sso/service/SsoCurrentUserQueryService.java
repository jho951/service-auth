package com.authservice.app.domain.auth.sso.service;

import com.authservice.app.domain.auth.sso.model.SsoPageType;
import com.authservice.app.domain.auth.sso.model.SsoPrincipal;
import com.authservice.app.domain.auth.sso.model.SsoStorePayloads.SsoSessionPayload;
import com.authservice.common.base.constant.ErrorCode;
import com.authservice.common.base.exception.GlobalException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SsoCurrentUserQueryService {

	private final SsoSessionStore sessionStore;
	private final SsoCookieService cookieService;
	private final AdminIpGuardService adminIpGuardService;

	public SsoCurrentUserQueryService(
		SsoSessionStore sessionStore,
		SsoCookieService cookieService,
		AdminIpGuardService adminIpGuardService
	) {
		this.sessionStore = sessionStore;
		this.cookieService = cookieService;
		this.adminIpGuardService = adminIpGuardService;
	}

	public SsoPrincipal getCurrentUser(HttpServletRequest request, String page) {
		guardAdminPageAccess(page, request);

		String sessionId = cookieService.extractSessionId(request)
			.orElseThrow(() -> new GlobalException(ErrorCode.NEED_LOGIN));

		SsoSessionPayload payload = sessionStore.findSession(sessionId)
			.orElseThrow(() -> new GlobalException(ErrorCode.NEED_LOGIN));

		return new SsoPrincipal(
			payload.getUserId(),
			payload.getEmail(),
			payload.getName(),
			payload.getAvatarUrl(),
			payload.getRoles() == null ? List.of() : payload.getRoles(),
			payload.getStatus()
		);
	}

	private void guardAdminPageAccess(String page, HttpServletRequest request) {
		if (page != null && !page.isBlank() && SsoPageType.from(page) == SsoPageType.ADMIN) {
			adminIpGuardService.validate(request);
		}
	}
}
