package com.authservice.app.domain.auth.internal.controller;

import com.authservice.app.domain.auth.sso.dto.SsoResponse;
import com.authservice.app.domain.auth.sso.service.SsoInternalSessionValidationService;
import com.authservice.app.config.swagger.SwaggerTag;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/auth/session")
@RequiredArgsConstructor
@Tag(name = SwaggerTag.AUTH, description = "Internal auth session controller")
public class InternalSessionController {

	private final SsoInternalSessionValidationService ssoInternalSessionValidationService;

	@PostMapping("/validate")
	public ResponseEntity<SsoResponse.InternalSessionValidationResponse> validateSession(HttpServletRequest request) {
		return ssoInternalSessionValidationService.validate(request);
	}
}
