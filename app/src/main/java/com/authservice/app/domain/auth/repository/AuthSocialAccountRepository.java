package com.authservice.app.domain.auth.repository;

import com.authservice.app.domain.auth.entity.AuthSocialAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSocialAccountRepository extends JpaRepository<AuthSocialAccount, UUID> {
	Optional<AuthSocialAccount> findByProviderAndProviderUserKey(String provider, String providerUserKey);
}
