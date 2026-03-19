package com.authservice.app.domain.auth.service;

import com.authservice.app.domain.auth.entity.AuthLoginAttempt;
import com.authservice.app.domain.auth.repository.AuthLoginAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthLoginAttemptService {

	private final AuthLoginAttemptRepository authLoginAttemptRepository;

	public AuthLoginAttemptService(AuthLoginAttemptRepository authLoginAttemptRepository) {
		this.authLoginAttemptRepository = authLoginAttemptRepository;
	}

	@Transactional
	public void record(String loginId, AuthRequestContext context, String result) {
		authLoginAttemptRepository.save(new AuthLoginAttempt(
			loginId == null ? "" : loginId,
			context == null ? "" : context.ip(),
			result
		));
	}
}
