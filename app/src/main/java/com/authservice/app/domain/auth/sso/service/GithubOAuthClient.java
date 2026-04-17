package com.authservice.app.domain.auth.sso.service;

import com.authservice.app.domain.auth.sso.config.SsoProperties;
import com.authservice.app.domain.auth.sso.model.GithubUserProfile;
import com.authservice.app.common.base.constant.ErrorCode;
import com.authservice.app.common.base.exception.GlobalException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GithubOAuthClient {

	private static final Logger log = LoggerFactory.getLogger(GithubOAuthClient.class);

	private final RestClient restClient;
	private final SsoProperties properties;

	public GithubOAuthClient(SsoProperties properties) {
		this.restClient = RestClient.builder().build();
		this.properties = properties;
	}

	public GithubUserProfile fetchUserProfileByAccessToken(String accessToken) {
		GithubUserResponse user = restClient.get()
			.uri(properties.getGithub().getUserUri())
			.header("Accept", MediaType.APPLICATION_JSON_VALUE)
			.header("Authorization", "Bearer " + accessToken)
			.retrieve()
			.body(GithubUserResponse.class);

		if (user == null || user.id() == null) {
			log.warn("GitHub user lookup failed: user payload missing");
			throw new GlobalException(ErrorCode.INVALID_REQUEST);
		}

		String email = user.email();
		if (email == null || email.isBlank()) {
			email = resolvePrimaryEmail(accessToken);
		}
		if (email == null || email.isBlank()) {
			log.warn("GitHub user lookup failed: verified email missing. githubId={}, login={}", user.id(), user.login());
			throw new GlobalException(ErrorCode.INVALID_REQUEST);
		}

		String name = user.name();
		if (name == null || name.isBlank()) {
			name = user.login();
		}

		return new GithubUserProfile(
			String.valueOf(user.id()),
			email,
			name,
			user.avatarUrl()
		);
	}

	private String resolvePrimaryEmail(String accessToken) {
		GithubEmailResponse[] emails = restClient.get()
			.uri(properties.getGithub().getEmailsUri())
			.header("Accept", MediaType.APPLICATION_JSON_VALUE)
			.header("Authorization", "Bearer " + accessToken)
			.retrieve()
			.body(GithubEmailResponse[].class);

		if (emails == null || emails.length == 0) {
			return null;
		}

		return Arrays.stream(emails)
			.filter(email -> Boolean.TRUE.equals(email.verified()))
			.sorted(Comparator.comparing((GithubEmailResponse email) -> Boolean.TRUE.equals(email.primary())).reversed())
			.map(GithubEmailResponse::email)
			.findFirst()
			.orElse(emails[0].email());
	}

	private record GithubUserResponse(Long id, String login, String name, String email,
									  @JsonProperty("avatar_url") String avatarUrl) {
	}

	private record GithubEmailResponse(String email, Boolean primary, Boolean verified) {
	}
}
