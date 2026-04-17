package com.authservice.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.authservice.app.domain.auth.entity.Auth;
import com.authservice.app.domain.auth.model.AuthUser;
import com.authservice.app.domain.auth.repository.AuthRepository;
import com.authservice.app.domain.auth.service.AuthUserCacheStore;
import com.authservice.app.domain.auth.service.AuthUserFinder;
import com.authservice.app.domain.auth.userdirectory.model.UserAccountProfile;
import com.authservice.app.domain.auth.userdirectory.service.UserDirectory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthUserFinderCacheTest {

	@Mock
	private AuthRepository authRepository;

	@Mock
	private UserDirectory userDirectory;

	@Mock
	private AuthUserCacheStore authUserCacheStore;

	private AuthUserFinder authUserFinder;

	@BeforeEach
	void setUp() {
		authUserFinder = new AuthUserFinder(authRepository, userDirectory, authUserCacheStore);
	}

	@Test
	void returnsCachedUserWhenRedisHit() {
		AuthUser cached = new AuthUser("user-id", "login-id", "pw-hash", List.of("USER"));
		when(authUserCacheStore.get("login-id")).thenReturn(Optional.of(cached));

		Optional<AuthUser> result = authUserFinder.findByUsername("login-id");

		assertThat(result).contains(cached);
		verify(authRepository, never()).findByLoginId(any());
		verify(userDirectory, never()).findByUserId(any());
	}

	@Test
	void loadsFromSourceAndWarmsCacheWhenRedisMiss() {
		UUID userId = UUID.randomUUID();
		Auth auth = Auth.builder()
			.userId(userId)
			.loginId("login-id")
			.passwordHash("pw-hash")
			.build();
		UserAccountProfile profile = new UserAccountProfile(userId, "a@b.com", "name", "ADMIN", "A", null);

		when(authUserCacheStore.get("login-id")).thenReturn(Optional.empty());
		when(authRepository.findByLoginId("login-id")).thenReturn(Optional.of(auth));
		when(userDirectory.findByUserId(userId)).thenReturn(Optional.of(profile));

		Optional<AuthUser> result = authUserFinder.findByUsername("login-id");

		assertThat(result).isPresent();
		assertThat(result.get().username()).isEqualTo("login-id");
		assertThat(result.get().roles()).containsExactly("ADMIN");
		verify(authUserCacheStore).put(any(), any(), any(), any());
	}

	@Test
	void acceptsShortActiveStatusFromUserService() {
		UUID userId = UUID.randomUUID();
		Auth auth = Auth.builder()
			.userId(userId)
			.loginId("short-active")
			.passwordHash("pw-hash")
			.build();
		UserAccountProfile profile = new UserAccountProfile(userId, "a@b.com", "name", "USER", "A", null);

		when(authUserCacheStore.get("short-active")).thenReturn(Optional.empty());
		when(authRepository.findByLoginId("short-active")).thenReturn(Optional.of(auth));
		when(userDirectory.findByUserId(userId)).thenReturn(Optional.of(profile));

		Optional<AuthUser> result = authUserFinder.findByUsername("short-active");

		assertThat(result).isPresent();
		assertThat(result.get().roles()).containsExactly("USER");
		verify(authUserCacheStore).put(any(), any(), any(), any());
	}

	@Test
	void rejectsLegacyActiveStringStatus() {
		UUID userId = UUID.randomUUID();
		Auth auth = Auth.builder()
			.userId(userId)
			.loginId("legacy-active")
			.passwordHash("pw-hash")
			.build();
		UserAccountProfile profile = new UserAccountProfile(userId, "a@b.com", "name", "USER", "ACTIVE", null);

		when(authUserCacheStore.get("legacy-active")).thenReturn(Optional.empty());
		when(authRepository.findByLoginId("legacy-active")).thenReturn(Optional.of(auth));
		when(userDirectory.findByUserId(userId)).thenReturn(Optional.of(profile));

		Optional<AuthUser> result = authUserFinder.findByUsername("legacy-active");

		assertThat(result).isEmpty();
		verify(authUserCacheStore, never()).put(any(), any(), any(), any());
	}
}
