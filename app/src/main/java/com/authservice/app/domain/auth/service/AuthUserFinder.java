package com.authservice.app.domain.auth.service;

import com.authservice.app.domain.auth.model.AuthUser;
import com.authservice.app.domain.auth.userdirectory.service.UserDirectory;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.authservice.app.domain.auth.repository.AuthRepository;

@Component
public class AuthUserFinder {
	private final AuthRepository authRepository;
	private final UserDirectory userDirectory;
	private final AuthUserCacheStore authUserCacheStore;

	public AuthUserFinder(AuthRepository authRepository, UserDirectory userDirectory, AuthUserCacheStore authUserCacheStore) {
		this.authRepository = authRepository;
		this.userDirectory = userDirectory;
		this.authUserCacheStore = authUserCacheStore;
	}

	public Optional<AuthUser> findByUsername(String username) {
		Optional<AuthUser> cached = authUserCacheStore.get(username);
		if (cached.isPresent()) {
			return cached;
		}

		return authRepository.findByLoginId(username)
			.filter(auth -> !auth.isAccountLocked())
			.flatMap(auth -> userDirectory.findByUserId(auth.getUserId())
				.filter(user -> "A".equalsIgnoreCase(user.status()))
				.map(user -> {
					AuthUser found = new AuthUser(
						String.valueOf(user.userId()),
						auth.getLoginId(),
						auth.getPasswordHash(),
						List.of(user.role())
					);
					authUserCacheStore.put(
						found.userId(),
						found.username(),
						auth.getPasswordHash(),
						found.roles()
					);
					return found;
				}));
	}
}
