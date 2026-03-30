package com.authservice.app.domain.auth.service;

import com.auth.api.model.User;
import com.auth.spi.UserFinder;
import com.authservice.app.domain.auth.userdirectory.service.UserDirectory;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.authservice.app.domain.auth.repository.AuthRepository;

@Component
public class AuthUserFinder implements UserFinder {
	private final AuthRepository authRepository;
	private final UserDirectory userDirectory;
	private final AuthUserCacheStore authUserCacheStore;

	public AuthUserFinder(AuthRepository authRepository, UserDirectory userDirectory, AuthUserCacheStore authUserCacheStore) {
		this.authRepository = authRepository;
		this.userDirectory = userDirectory;
		this.authUserCacheStore = authUserCacheStore;
	}

	@Override
	public Optional<User> findByUsername(String username) {
		Optional<User> cached = authUserCacheStore.get(username);
		if (cached.isPresent()) {
			return cached;
		}

		return authRepository.findByLoginId(username)
			.filter(auth -> !auth.isAccountLocked())
			.flatMap(auth -> userDirectory.findByUserId(auth.getUserId())
				.filter(user -> "A".equalsIgnoreCase(user.status()))
				.map(user -> {
					User found = new User(
						String.valueOf(user.userId()),
						auth.getLoginId(),
						auth.getPasswordHash(),
						List.of(user.role())
					);
					authUserCacheStore.put(
						found.getUserId(),
						found.getUsername(),
						auth.getPasswordHash(),
						found.getRoles()
					);
					return found;
				}));
	}
}
