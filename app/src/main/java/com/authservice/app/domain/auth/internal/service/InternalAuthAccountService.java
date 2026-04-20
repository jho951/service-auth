package com.authservice.app.domain.auth.internal.service;

import com.authservice.common.base.constant.ErrorCode;
import com.authservice.common.base.exception.GlobalException;
import com.authservice.app.domain.audit.service.AuthAuditLogService;
import com.authservice.app.domain.auth.entity.Auth;
import com.authservice.app.domain.auth.internal.dto.InternalAuthRequest;
import com.authservice.app.domain.auth.internal.dto.InternalAuthResponse;
import com.authservice.app.domain.auth.repository.AuthRepository;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalAuthAccountService {

	private final AuthRepository authRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthAuditLogService authAuditLogService;

	@Transactional
	public InternalAuthResponse.AccountResponse createAccount(InternalAuthRequest.CreateAccountRequest request) {
		try {
			if (authRepository.findByUserId(request.getUserId()).isPresent()
				|| authRepository.findByUsername(request.getLoginId()).isPresent()) {
				throw new GlobalException(ErrorCode.CONFLICT_AUTH_ACCOUNT);
			}

			Auth auth = authRepository.save(Auth.builder()
				.userId(request.getUserId())
				.loginId(request.getLoginId())
				.passwordHash(passwordEncoder.encode(request.getPassword()))
				.build());
			authAuditLogService.logInternalAccountCreate(String.valueOf(auth.getUserId()), auth.getUsername());

			return InternalAuthResponse.AccountResponse.from(auth.getId(), auth.getUserId(), auth.getUsername());
		} catch (DataIntegrityViolationException e) {
			throw new GlobalException(ErrorCode.CONFLICT_AUTH_ACCOUNT);
		} catch (PersistenceException e) {
			throw new GlobalException(ErrorCode.INVALID_REQUEST_DATA);
		}
	}

	@Transactional
	public void deleteAccount(java.util.UUID userId) {
		Auth auth = authRepository.findByUserId(userId)
			.orElseThrow(() -> new GlobalException(ErrorCode.NOT_FOUND_AUTH_ACCOUNT));
		authRepository.delete(auth);
		authAuditLogService.logInternalAccountDelete(String.valueOf(userId));
	}
}
