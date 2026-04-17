package com.authservice.app.domain.auth.sso.service;

import com.authservice.app.domain.auth.sso.model.GithubUserProfile;
import com.authservice.app.domain.auth.sso.model.SsoPrincipal;
import com.authservice.app.common.base.constant.ErrorCode;
import com.authservice.app.common.base.exception.GlobalException;
import com.authservice.app.domain.auth.userdirectory.model.OAuth2ProvisionCommand;
import com.authservice.app.domain.auth.userdirectory.model.UserAccountProfile;
import com.authservice.app.domain.auth.userdirectory.service.UserDirectory;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SsoUserService {

	private final UserDirectory userDirectory;

	public SsoUserService(UserDirectory userDirectory) {
		this.userDirectory = userDirectory;
	}

	public SsoPrincipal verifyGithubUser(GithubUserProfile profile) {
		return verifyGithubProfile(profile);
	}

	private SsoPrincipal verifyGithubProfile(GithubUserProfile profile) {
		UserAccountProfile user = userDirectory.provisionOAuth2User(new OAuth2ProvisionCommand(
			"github",
			profile.getProviderId(),
			profile.getEmail(),
			profile.getName(),
			profile.getAvatarUrl()
		));

		return new SsoPrincipal(
			user.userId().toString(),
			user.email(),
			user.name(),
			user.avatarUrl(),
			List.of(user.role()),
			user.status()
		);
	}
}
