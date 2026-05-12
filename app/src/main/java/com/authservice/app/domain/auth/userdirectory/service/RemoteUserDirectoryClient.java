package com.authservice.app.domain.auth.userdirectory.service;

import com.authservice.app.domain.auth.userdirectory.config.UserServiceProperties;
import com.authservice.app.domain.auth.userdirectory.model.UserAccountProfile;
import com.authservice.common.base.constant.ErrorCode;
import com.authservice.common.base.exception.GlobalException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class RemoteUserDirectoryClient implements UserDirectoryClient {

	private final RestClient restClient;
	private final UserServiceProperties userServiceProperties;
	private final InternalServiceTokenIssuer internalServiceTokenIssuer;

	public RemoteUserDirectoryClient(
		RestClient.Builder restClientBuilder,
		UserServiceProperties userServiceProperties,
		InternalServiceTokenIssuer internalServiceTokenIssuer
	) {
		this.restClient = restClientBuilder
			.baseUrl(userServiceProperties.getBaseUrl() == null ? "" : userServiceProperties.getBaseUrl())
			.build();
		this.userServiceProperties = userServiceProperties;
		this.internalServiceTokenIssuer = internalServiceTokenIssuer;
	}

	@Override
	public Optional<UserAccountProfile> findByUserId(UUID userId) {
		return executeOptional(() -> restClient.get()
				.uri("/internal/users/{userId}", userId)
				.header(HttpHeaders.AUTHORIZATION, bearerToken())
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.body(userDetailResponseType()))
			.map(UserDetailResponse::toProfile);
	}

	@Override
	public UserAccountProfile provisionSocialUser(String socialType, String providerUserId, String email) {
		return executeRequired(() -> restClient.post()
				.uri("/internal/users/find-or-create-and-link-social")
				.header(HttpHeaders.AUTHORIZATION, bearerToken())
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(new FindOrCreateAndLinkSocialRequest(email, socialType, providerUserId))
				.retrieve()
				.body(userDetailResponseType()))
			.toProfile();
	}

	private void validateBaseUrl() {
		if (userServiceProperties.getBaseUrl() == null || userServiceProperties.getBaseUrl().isBlank()) {
			throw new GlobalException(ErrorCode.USER_SERVICE_UNAVAILABLE);
		}
	}

	private String bearerToken() {
		return internalServiceTokenIssuer.issueBearerToken();
	}

	private Optional<UserDetailResponse> executeOptional(Supplier<GlobalResponse<UserDetailResponse>> requestCall) {
		validateBaseUrl();
		try {
			return Optional.ofNullable(requestCall.get()).map(GlobalResponse::data);
		} catch (HttpClientErrorException.NotFound ex) {
			return Optional.empty();
		} catch (RuntimeException ex) {
			throw unavailable();
		}
	}

	private UserDetailResponse executeRequired(Supplier<GlobalResponse<UserDetailResponse>> requestCall) {
		validateBaseUrl();
		try {
			return requireData(requestCall.get());
		} catch (RuntimeException ex) {
			if (ex instanceof GlobalException) {
				throw ex;
			}
			throw unavailable();
		}
	}

	private UserDetailResponse requireData(GlobalResponse<UserDetailResponse> response) {
		if (response == null || response.data() == null) {
			throw unavailable();
		}
		return response.data();
	}

	private GlobalException unavailable() {
		return new GlobalException(ErrorCode.USER_SERVICE_UNAVAILABLE);
	}

	private static org.springframework.core.ParameterizedTypeReference<GlobalResponse<UserDetailResponse>> userDetailResponseType() {
		return new org.springframework.core.ParameterizedTypeReference<>() {
		};
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
