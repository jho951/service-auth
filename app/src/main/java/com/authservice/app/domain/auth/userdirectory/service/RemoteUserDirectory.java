package com.authservice.app.domain.auth.userdirectory.service;

import com.authservice.app.domain.auth.userdirectory.model.OAuth2ProvisionCommand;
import com.authservice.app.domain.auth.userdirectory.model.UserAccountProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RemoteUserDirectory implements UserDirectory {

	private final UserDirectoryClient userDirectoryClient;

	public RemoteUserDirectory(UserDirectoryClient userDirectoryClient) {
		this.userDirectoryClient = userDirectoryClient;
	}

	@Override
	public Optional<UserAccountProfile> findByUserId(UUID userId) {
		return userDirectoryClient.findByUserId(userId);
	}

	@Override
	public UserAccountProfile provisionOAuth2User(OAuth2ProvisionCommand command) {
		return enrichProfile(
			userDirectoryClient.provisionSocialUser(command.provider().socialType(), command.providerUserId(), command.email()),
			command
		);
	}

	private UserAccountProfile enrichProfile(UserAccountProfile profile, OAuth2ProvisionCommand command) {
		return new UserAccountProfile(
			profile.userId(),
			profile.email(),
			command.name(),
			profile.role(),
			profile.status(),
			command.avatarUrl()
		);
	}
}
