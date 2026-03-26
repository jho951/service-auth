package com.authservice.app.domain.auth.sso.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class SsoOAuth2FailureHandler implements AuthenticationFailureHandler {

	private final SsoAuthService ssoAuthService;
	private final SsoCookieService ssoCookieService;

	public SsoOAuth2FailureHandler(SsoAuthService ssoAuthService, SsoCookieService ssoCookieService) {
		this.ssoAuthService = ssoAuthService;
		this.ssoCookieService = ssoCookieService;
	}

	@Override
	public void onAuthenticationFailure(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException exception
	) throws IOException, ServletException {
		URI redirectUri = ssoAuthService.resolveOAuthFailureRedirect(request);
		response.addHeader("Set-Cookie", ssoCookieService.clearOAuthStateCookie());

		if (redirectUri == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.getWriter().write("{\"error\":\"OAuth authentication failed\"}");
			return;
		}

		response.setStatus(HttpServletResponse.SC_FOUND);
		response.setHeader("Location", redirectUri.toString());
	}
}
