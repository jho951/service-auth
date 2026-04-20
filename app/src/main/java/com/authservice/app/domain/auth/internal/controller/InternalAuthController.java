package com.authservice.app.domain.auth.internal.controller;

import com.authservice.common.base.constant.SuccessCode;
import com.authservice.common.base.dto.GlobalResponse;
import com.authservice.app.domain.auth.internal.dto.InternalAuthRequest;
import com.authservice.app.domain.auth.internal.dto.InternalAuthResponse;
import com.authservice.app.domain.auth.internal.service.InternalAuthAccountService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.authservice.common.swagger.constant.SwaggerTag;

@RestController
@RequestMapping("/internal/auth/accounts")
@RequiredArgsConstructor
@Tag(name = SwaggerTag.AUTH, description = "Internal auth account controller")
public class InternalAuthController {

	private final InternalAuthAccountService internalAuthAccountService;

	@PostMapping
	public ResponseEntity<GlobalResponse<InternalAuthResponse.AccountResponse>> createAccount(
		@Valid @RequestBody InternalAuthRequest.CreateAccountRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(GlobalResponse.ok(
				SuccessCode.CREATE_SUCCESS,
				internalAuthAccountService.createAccount(request)
			));
	}

	@DeleteMapping("/{userId}")
	public GlobalResponse<Void> deleteAccount(
		@PathVariable UUID userId
	) {
		internalAuthAccountService.deleteAccount(userId);
		return GlobalResponse.ok(SuccessCode.DELETE_SUCCESS);
	}
}
