package com.authservice.app.domain.auth.sso.controller;

import com.authservice.app.domain.auth.sso.dto.SsoRequest;
import com.authservice.app.domain.auth.sso.dto.SsoResponse;
import com.authservice.app.domain.auth.sso.model.SsoPrincipal;
import com.authservice.app.domain.auth.sso.service.SsoCurrentUserQueryService;
import com.authservice.app.domain.auth.sso.service.SsoInternalSessionValidationService;
import com.authservice.app.domain.auth.sso.service.SsoLogoutService;
import com.authservice.app.domain.auth.sso.service.SsoOAuthFlowService;
import com.authservice.app.domain.auth.sso.service.SsoTicketExchangeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/auth")
public class SsoController {

	private final SsoOAuthFlowService ssoOAuthFlowService;
	private final SsoTicketExchangeService ssoTicketExchangeService;
	private final SsoInternalSessionValidationService ssoInternalSessionValidationService;
	private final SsoCurrentUserQueryService ssoCurrentUserQueryService;
	private final SsoLogoutService ssoLogoutService;

	public SsoController(
		SsoOAuthFlowService ssoOAuthFlowService,
		SsoTicketExchangeService ssoTicketExchangeService,
		SsoInternalSessionValidationService ssoInternalSessionValidationService,
		SsoCurrentUserQueryService ssoCurrentUserQueryService,
		SsoLogoutService ssoLogoutService
	) {
		this.ssoOAuthFlowService = ssoOAuthFlowService;
		this.ssoTicketExchangeService = ssoTicketExchangeService;
		this.ssoInternalSessionValidationService = ssoInternalSessionValidationService;
		this.ssoCurrentUserQueryService = ssoCurrentUserQueryService;
		this.ssoLogoutService = ssoLogoutService;
	}

	@GetMapping({"/sso/start", "/login/github", "/oauth2/authorize/github"})
	public ResponseEntity<Void> startGithubLogin(
		@RequestParam(name = "page", required = false) String page,
		@RequestParam(name = "redirect_uri", required = false) String redirectUri,
		HttpServletRequest request
	) {
		return ssoOAuthFlowService.startGithubLogin(page, redirectUri, request);
	}

	@GetMapping("/oauth/github/callback")
	public ResponseEntity<Void> oauthGithubCallback(HttpServletRequest request) {
		String query = request.getQueryString();
		String location = UriComponentsBuilder.fromPath("/login/oauth2/code/github")
			.query(query)
			.build(true)
			.toUriString();
		return ResponseEntity.status(302).header("Location", location).build();
	}

	@PostMapping("/exchange")
	public ResponseEntity<Void> exchange(@Valid @RequestBody SsoRequest.ExchangeRequest request, HttpServletRequest servletRequest) {
		return ssoTicketExchangeService.exchangeTicket(request.getTicket(), servletRequest);
	}

	@GetMapping("/session")
	public ResponseEntity<SsoResponse.InternalSessionValidationResponse> session(HttpServletRequest request) {
		return ssoInternalSessionValidationService.validate(request);
	}

	@GetMapping("/me")
	public SsoResponse.MeResponse me(
		HttpServletRequest request,
		@RequestParam(name = "page", required = false) String page
	) {
		SsoPrincipal principal = ssoCurrentUserQueryService.getCurrentUser(request, page);
		return new SsoResponse.MeResponse(
			principal.getUserId(),
			principal.getEmail(),
			principal.getName(),
			principal.getAvatarUrl(),
			principal.getRoles(),
			principal.getStatus()
		);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		return ssoLogoutService.logout(request);
	}
}
