package com.authservice.app.domain.auth.service;

import com.authservice.app.domain.auth.entity.Auth;
import com.authservice.app.domain.auth.repository.AuthRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthAccountPolicyService {

	private static final int MAX_FAILED_LOGIN_COUNT = 5;

	private final AuthRepository authRepository;

	public AuthAccountPolicyService(AuthRepository authRepository) {
		this.authRepository = authRepository;
	}

	@Transactional
	public Optional<Auth> markLoginSuccess(String loginId) {
		return authRepository.findByLoginId(loginId)
			.map(auth -> {
				auth.markLoginSuccess();
				return authRepository.save(auth);
			});
	}

	@Transactional
	public Optional<Auth> markLoginFailure(String loginId) {
		return authRepository.findByLoginId(loginId)
			.map(auth -> {
				auth.markLoginFailure();
				if (auth.getFailedLoginCount() >= MAX_FAILED_LOGIN_COUNT) {
					auth.lockAccount();
				}
				return authRepository.save(auth);
			});
	}
}
