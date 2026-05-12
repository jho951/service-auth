package com.authservice.common.base.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

class AuditorAwareImplTest {

	private final AuditorAwareImpl auditorAware = new AuditorAwareImpl();

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void returnsEmptyWhenAuthenticationMissing() {
		assertTrue(auditorAware.getCurrentAuditor().isEmpty());
	}

	@Test
	void returnsEmptyWhenAuthenticationUnauthenticated() {
		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken("alice", "secret");
		SecurityContextHolder.getContext().setAuthentication(authentication);

		assertTrue(auditorAware.getCurrentAuditor().isEmpty());
	}

	@Test
	void returnsAuthenticationNameWhenAuthenticated() {
		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken("alice", "secret", AuthorityUtils.NO_AUTHORITIES);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		Optional<String> auditor = auditorAware.getCurrentAuditor();

		assertTrue(auditor.isPresent());
		assertEquals("alice", auditor.get());
	}
}
