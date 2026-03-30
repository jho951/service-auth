package com.authservice.app.domain.auth.userdirectory.service;

import com.authservice.app.common.base.constant.ErrorCode;
import com.authservice.app.common.base.exception.GlobalException;
import com.authservice.app.domain.auth.userdirectory.config.UserServiceProperties;
import com.authservice.app.domain.auth.userdirectory.model.OAuth2ProvisionCommand;
import com.authservice.app.domain.auth.userdirectory.model.UserAccountProfile;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Optional;
import java.util.UUID;
import java.util.Date;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;

@Service
public class RemoteUserDirectory implements UserDirectory {

	private final RestClient restClient;
	private final UserServiceProperties userServiceProperties;
	private final Key internalJwtKey;

	public RemoteUserDirectory(UserServiceProperties userServiceProperties) {
		this.userServiceProperties = userServiceProperties;
		this.restClient = RestClient.builder()
			.baseUrl(userServiceProperties.getBaseUrl() == null ? "" : userServiceProperties.getBaseUrl())
			.build();
		this.internalJwtKey = buildInternalJwtKey(userServiceProperties);
	}

	@Override
	public Optional<UserAccountProfile> findByUserId(UUID userId) {
		validateBaseUrl();
		try {
			GlobalResponse<UserDetailResponse> response = restClient.get()
				.uri("/internal/users/{userId}", userId)
				.header(HttpHeaders.AUTHORIZATION, bearerToken())
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.body(userDetailResponseType());
			return Optional.ofNullable(response)
				.map(GlobalResponse::data)
				.map(UserDetailResponse::toProfile);
		} catch (HttpClientErrorException.NotFound e) {
			return Optional.empty();
		} catch (RuntimeException e) {
			throw new GlobalException(ErrorCode.USER_SERVICE_UNAVAILABLE);
		}
	}

	@Override
	public UserAccountProfile provisionOAuth2User(OAuth2ProvisionCommand command) {
		validateBaseUrl();
		try {
			GlobalResponse<UserDetailResponse> response = restClient.post()
				.uri("/internal/users/find-or-create-and-link-social")
				.header(HttpHeaders.AUTHORIZATION, bearerToken())
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(new FindOrCreateAndLinkSocialRequest(
					command.email(),
					toSocialType(command.provider()),
					command.providerUserId()
				))
				.retrieve()
				.body(userDetailResponseType());
			if (response == null || response.data() == null) {
				throw new GlobalException(ErrorCode.USER_SERVICE_UNAVAILABLE);
			}
			return enrichProfile(response.data().toProfile(), command);
		} catch (RuntimeException e) {
			throw new GlobalException(ErrorCode.USER_SERVICE_UNAVAILABLE);
		}
	}

	private void validateBaseUrl() {
		if (userServiceProperties.getBaseUrl() == null || userServiceProperties.getBaseUrl().isBlank()) {
			throw new GlobalException(ErrorCode.USER_SERVICE_UNAVAILABLE);
		}
	}

	private String toSocialType(String provider) {
		if ("github".equalsIgnoreCase(provider)) {
			return "GITHUB";
		}
		throw new GlobalException(ErrorCode.INVALID_REQUEST);
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

	private static org.springframework.core.ParameterizedTypeReference<GlobalResponse<UserDetailResponse>> userDetailResponseType() {
		return new org.springframework.core.ParameterizedTypeReference<>() {
		};
	}

	private String bearerToken() {
		return "Bearer " + issueInternalServiceToken();
	}

	private String issueInternalServiceToken() {
		UserServiceProperties.Jwt jwt = userServiceProperties.getJwt();
		if (jwt == null || jwt.getSecret() == null || jwt.getSecret().isBlank()) {
			throw new GlobalException(ErrorCode.USER_SERVICE_UNAVAILABLE);
		}

		Date issuedAt = new Date();
		Date expiration = new Date(issuedAt.getTime() + (Math.max(jwt.getTtlSeconds(), 1) * 1000L));

		return Jwts.builder()
			.setIssuer(jwt.getIssuer())
			.setAudience(jwt.getAudience())
			.setSubject(jwt.getSubject())
			.claim("scope", jwt.getScope())
			.setIssuedAt(issuedAt)
			.setExpiration(expiration)
			.signWith(internalJwtKey, SignatureAlgorithm.HS256)
			.compact();
	}

	private static Key buildInternalJwtKey(UserServiceProperties properties) {
		UserServiceProperties.Jwt jwt = properties.getJwt();
		if (jwt == null || jwt.getSecret() == null || jwt.getSecret().isBlank()) {
			return null;
		}
		byte[] secretBytes = jwt.getSecret().getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < 32) {
			throw new GlobalException(ErrorCode.USER_SERVICE_UNAVAILABLE);
		}
		return Keys.hmacShaKeyFor(secretBytes);
	}

	private record GlobalResponse<T>(
		int httpStatus,
		boolean success,
		String message,
		int code,
		T data
	) {
	}

	private record UserDetailResponse(
		UUID id,
		String email,
		String role,
		String status
	) {
		private UserAccountProfile toProfile() {
			return new UserAccountProfile(id, email, null, role, status, null);
		}
	}

	private record FindOrCreateAndLinkSocialRequest(
		String email,
		String socialType,
		String providerId
	) {
	}
}
