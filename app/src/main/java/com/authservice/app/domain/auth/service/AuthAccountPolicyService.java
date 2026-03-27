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
	private final AuthUserCacheStore authUserCacheStore;

	public AuthAccountPolicyService(AuthRepository authRepository, AuthUserCacheStore authUserCacheStore) {
		this.authRepository = authRepository;
		this.authUserCacheStore = authUserCacheStore;
	}

	@Transactional
	public Optional<Auth> markLoginSuccess(String loginId) {
		return authRepository.findByLoginId(loginId)
			.map(auth -> {
				auth.markLoginSuccess();
				Auth saved = authRepository.save(auth);
				authUserCacheStore.evict(loginId);
				return saved;
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
				Auth saved = authRepository.save(auth);
				authUserCacheStore.evict(loginId);
				return saved;
			});
	}
}
