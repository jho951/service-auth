package com.authservice.app.domain.auth.sso.service;

import com.authservice.app.domain.auth.sso.model.GithubUserProfile;
import com.authservice.app.domain.auth.sso.model.SsoPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class SsoOAuth2SuccessHandler implements AuthenticationSuccessHandler {

	private static final Logger log = LoggerFactory.getLogger(SsoOAuth2SuccessHandler.class);

	private final SsoAuthService ssoAuthService;
	private final SsoUserService ssoUserService;
	private final SsoCookieService ssoCookieService;
	private final GithubOAuthClient githubOAuthClient;
	private final OAuth2AuthorizedClientService authorizedClientService;

	public SsoOAuth2SuccessHandler(
		SsoAuthService ssoAuthService,
		SsoUserService ssoUserService,
		SsoCookieService ssoCookieService,
		GithubOAuthClient githubOAuthClient,
		OAuth2AuthorizedClientService authorizedClientService
	) {
		this.ssoAuthService = ssoAuthService;
		this.ssoUserService = ssoUserService;
		this.ssoCookieService = ssoCookieService;
		this.githubOAuthClient = githubOAuthClient;
		this.authorizedClientService = authorizedClientService;
	}

	@Override
	public void onAuthenticationSuccess(
		HttpServletRequest request,
		HttpServletResponse response,
		Authentication authentication
	) throws IOException, ServletException {
		try {
			if (!(authentication instanceof OAuth2AuthenticationToken token)) {
				throw new ServletException("Unsupported authentication type: " + authentication.getClass().getName());
			}

			OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
				token.getAuthorizedClientRegistrationId(),
				token.getName()
			);
			if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
				throw new ServletException("OAuth2 authorized client access token is missing");
			}

			GithubUserProfile profile = githubOAuthClient.fetchUserProfileByAccessToken(
				authorizedClient.getAccessToken().getTokenValue()
			);
			SsoPrincipal principal = ssoUserService.verifyGithubUser(profile);
			URI redirectUri = ssoAuthService.completeOAuthLogin(principal, request);

			response.setStatus(HttpServletResponse.SC_FOUND);
			response.setHeader("Location", redirectUri.toString());
			response.addHeader("Set-Cookie", ssoCookieService.clearOAuthStateCookie());
		} catch (RuntimeException ex) {
			log.warn("OAuth2 login success handler failed", ex);
			URI redirectUri = ssoAuthService.resolveOAuthFailureRedirect(request);
			response.addHeader("Set-Cookie", ssoCookieService.clearOAuthStateCookie());
			if (redirectUri == null) {
				response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
				response.setContentType("application/json");
				response.getWriter().write("{\"error\":\"OAuth user provisioning failed\"}");
				return;
			}
			response.setStatus(HttpServletResponse.SC_FOUND);
			response.setHeader("Location", redirectUri.toString());
		}
	}
}
