package com.authservice.app.domain.auth.internal.service;

import com.authservice.app.common.base.constant.ErrorCode;
import com.authservice.app.common.base.exception.GlobalException;
import com.authservice.app.domain.auth.entity.Auth;
import com.authservice.app.domain.auth.internal.dto.InternalAuthRequest;
import com.authservice.app.domain.auth.internal.dto.InternalAuthResponse;
import com.authservice.app.domain.auth.repository.AuthRepository;
import com.authservice.app.domain.auth.service.AuthAuditService;
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
	private final AuthAuditService authAuditService;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public InternalAuthResponse.AccountResponse createAccount(InternalAuthRequest.CreateAccountRequest request) {
		try {
			if (authRepository.findByUserId(request.getUserId()).isPresent()
				|| authRepository.findByUsername(request.getEmail()).isPresent()) {
				throw new GlobalException(ErrorCode.CONFLICT_AUTH_ACCOUNT);
			}

			Auth auth = authRepository.save(Auth.builder()
				.userId(request.getUserId())
				.loginId(request.getEmail())
				.email(request.getEmail())
				.passwordHash(passwordEncoder.encode(request.getPassword()))
				.build());

			authAuditService.log("ACCOUNT_CREATED", "SUCCESS", auth, null, "{\"source\":\"internal-signup\"}");

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
		authAuditService.log("ACCOUNT_DELETED", "SUCCESS", auth, null, "{\"source\":\"internal-signup-rollback\"}");
		authRepository.delete(auth);
	}
}
