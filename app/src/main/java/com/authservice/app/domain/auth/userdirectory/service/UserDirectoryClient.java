package com.authservice.app.domain.auth.userdirectory.service;

import com.authservice.app.domain.auth.userdirectory.model.UserAccountProfile;
import java.util.Optional;
import java.util.UUID;

public interface UserDirectoryClient {
	Optional<UserAccountProfile> findByUserId(UUID userId);
	UserAccountProfile provisionSocialUser(String socialType, String providerUserId, String email);
}
