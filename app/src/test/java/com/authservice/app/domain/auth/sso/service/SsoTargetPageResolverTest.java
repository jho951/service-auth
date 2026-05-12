package com.authservice.app.domain.auth.sso.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.authservice.app.domain.auth.sso.config.SsoProperties;
import com.authservice.app.domain.auth.sso.model.SsoPageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SsoTargetPageResolverTest {

	private SsoTargetPageResolver resolver;

	@BeforeEach
	void setUp() {
		resolver = new SsoTargetPageResolver(new SsoProperties());
	}

	@Test
	void resolvesConfiguredPageWhenPageParameterIsPresent() {
		var targetPage = resolver.resolve("editor", "http://localhost:5173/auth/callback");

		assertThat(targetPage.pageType()).isEqualTo(SsoPageType.EDITOR);
		assertThat(targetPage.redirectUri()).isEqualTo("http://localhost:5173/auth/callback");
	}

	@Test
	void resolvesPageFromRedirectUriWhenPageParameterIsMissing() {
		var targetPage = resolver.resolve(null, "http://localhost:5173/admin/auth/callback?ignored=true");

		assertThat(targetPage.pageType()).isEqualTo(SsoPageType.ADMIN);
		assertThat(targetPage.redirectUri()).isEqualTo("http://localhost:5173/admin/auth/callback");
	}

	@Test
	void rejectsUnexpectedRedirectUriForExplicitPage() {
		assertThatThrownBy(() -> resolver.resolve("editor", "http://localhost:3000/auth/callback"))
			.isInstanceOf(RuntimeException.class);
	}
}
