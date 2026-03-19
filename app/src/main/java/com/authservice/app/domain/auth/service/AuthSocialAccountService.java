package com.authservice.app.domain.auth.service;

import com.authservice.app.domain.auth.entity.AuthSocialAccount;
import com.authservice.app.domain.auth.repository.AuthSocialAccountRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthSocialAccountService {

	private final AuthSocialAccountRepository authSocialAccountRepository;

	public AuthSocialAccountService(AuthSocialAccountRepository authSocialAccountRepository) {
		this.authSocialAccountRepository = authSocialAccountRepository;
	}

	@Transactional
	public void upsert(String provider, String providerUserKey, UUID userId, String providerEmail) {
		authSocialAccountRepository.findByProviderAndProviderUserKey(provider, providerUserKey)
			.ifPresentOrElse(existing -> {
				existing.refreshLink(userId, providerEmail);
				authSocialAccountRepository.save(existing);
			}, () -> authSocialAccountRepository.save(
				new AuthSocialAccount(userId, provider, providerUserKey, providerEmail)
			));
	}
}
