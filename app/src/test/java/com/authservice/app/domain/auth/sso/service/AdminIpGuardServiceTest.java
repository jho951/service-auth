package com.authservice.app.domain.auth.sso.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.authservice.common.base.exception.GlobalException;
import com.authservice.app.domain.auth.sso.config.SsoProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AdminIpGuardServiceTest {

	@Test
	void validateBlocksDisallowedIpWhenEnabled() {
		SsoProperties properties = new SsoProperties();
		properties.getFrontend().getAdmin().getIpGuard().setEnabled(true);
		properties.getFrontend().getAdmin().getIpGuard().setDefaultAllow(false);
		properties.getFrontend().getAdmin().getIpGuard().setRules("127.0.0.1");
		AdminIpGuardService service = new AdminIpGuardService(properties);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("203.0.113.10");

		assertThatThrownBy(() -> service.validate(request))
			.isInstanceOf(GlobalException.class);
	}

	@Test
	void validateReadsRulesFromFileWhenConfigured(@TempDir Path tempDir) throws Exception {
		Path rulesFile = tempDir.resolve("ip-allow.txt");
		Files.writeString(rulesFile, "127.0.0.1\n");

		SsoProperties properties = new SsoProperties();
		properties.getFrontend().getAdmin().getIpGuard().setEnabled(true);
		properties.getFrontend().getAdmin().getIpGuard().setDefaultAllow(false);
		properties.getFrontend().getAdmin().getIpGuard().setRules("203.0.113.10");
		properties.getFrontend().getAdmin().getIpGuard().setRulesFile(rulesFile.toString());
		AdminIpGuardService service = new AdminIpGuardService(properties);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("203.0.113.10");

		assertThatThrownBy(() -> service.validate(request))
			.isInstanceOf(GlobalException.class);
	}

	@Test
	void validateReloadsRulesFileOnEachCheck(@TempDir Path tempDir) throws Exception {
		Path rulesFile = tempDir.resolve("ip-allow.txt");
		Files.writeString(rulesFile, "127.0.0.1\n");

		SsoProperties properties = new SsoProperties();
		properties.getFrontend().getAdmin().getIpGuard().setEnabled(true);
		properties.getFrontend().getAdmin().getIpGuard().setDefaultAllow(false);
		properties.getFrontend().getAdmin().getIpGuard().setRulesFile(rulesFile.toString());
		AdminIpGuardService service = new AdminIpGuardService(properties);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("203.0.113.10");

		assertThatThrownBy(() -> service.validate(request))
			.isInstanceOf(GlobalException.class);

		Files.writeString(rulesFile, "203.0.113.10\n");

		service.validate(request);
	}

	@Test
	void validateFailsClosedWhenRulesFileCannotBeRead() {
		SsoProperties properties = new SsoProperties();
		properties.getFrontend().getAdmin().getIpGuard().setEnabled(true);
		properties.getFrontend().getAdmin().getIpGuard().setDefaultAllow(true);
		properties.getFrontend().getAdmin().getIpGuard().setRulesFile("/path/does/not/exist/ip-allow.txt");
		AdminIpGuardService service = new AdminIpGuardService(properties);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("203.0.113.10");

		assertThatThrownBy(() -> service.validate(request))
			.isInstanceOf(GlobalException.class);
	}
}
