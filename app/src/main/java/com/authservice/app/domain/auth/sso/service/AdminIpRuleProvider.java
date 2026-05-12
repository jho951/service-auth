package com.authservice.app.domain.auth.sso.service;

import com.authservice.app.domain.auth.sso.config.SsoProperties;
import com.authservice.common.base.constant.ErrorCode;
import com.authservice.common.base.exception.GlobalException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AdminIpRuleProvider {

	private static final Logger log = LoggerFactory.getLogger(AdminIpRuleProvider.class);

	private volatile CachedRuleSet cachedRuleSet;

	public AdminIpRuleSet currentRuleSet(SsoProperties.AdminIpGuard properties) {
		String rulesFile = properties.getRulesFile();
		if (rulesFile == null || rulesFile.isBlank()) {
			return currentInlineRuleSet(properties);
		}

		try {
			Path path = Path.of(rulesFile.trim());
			FileFingerprint fingerprint = new FileFingerprint(
				path.toAbsolutePath().normalize().toString(),
				Files.getLastModifiedTime(path).toMillis(),
				Files.size(path)
			);

			CachedRuleSet cached = cachedRuleSet;
			if (cached != null && cached.matches(fingerprint)) {
				return cached.ruleSet();
			}

			AdminIpRuleSet ruleSet = AdminIpRuleSet.compile(loadRules(path));
			cachedRuleSet = CachedRuleSet.of(fingerprint, ruleSet);
			return ruleSet;
		} catch (IOException ex) {
			log.warn("Admin IP guard rules file could not be read. path={}", rulesFile, ex);
			throw new GlobalException(ErrorCode.FORBIDDEN);
		}
	}

	private AdminIpRuleSet currentInlineRuleSet(SsoProperties.AdminIpGuard properties) {
		String signature = properties.getRules() == null ? "" : properties.getRules().trim();
		CachedRuleSet cached = cachedRuleSet;
		if (cached != null && cached.matches(signature)) {
			return cached.ruleSet();
		}

		AdminIpRuleSet ruleSet = AdminIpRuleSet.compile(properties.parseRules());
		cachedRuleSet = CachedRuleSet.of(signature, ruleSet);
		return ruleSet;
	}

	private List<String> loadRules(Path path) throws IOException {
		return Files.readString(path, StandardCharsets.UTF_8)
			.lines()
			.map(String::trim)
			.filter(value -> !value.isBlank())
			.toList();
	}

	private record CachedRuleSet(String sourceKey, long lastModifiedAt, long size, AdminIpRuleSet ruleSet) {
		private static CachedRuleSet of(String sourceKey, AdminIpRuleSet ruleSet) {
			return new CachedRuleSet(sourceKey, -1L, -1L, ruleSet);
		}

		private static CachedRuleSet of(FileFingerprint fingerprint, AdminIpRuleSet ruleSet) {
			return new CachedRuleSet(fingerprint.sourceKey(), fingerprint.lastModifiedAt(), fingerprint.size(), ruleSet);
		}

		private boolean matches(String inlineSourceKey) {
			return sourceKey.equals(inlineSourceKey) && lastModifiedAt < 0;
		}

		private boolean matches(FileFingerprint fingerprint) {
			return sourceKey.equals(fingerprint.sourceKey())
				&& lastModifiedAt == fingerprint.lastModifiedAt()
				&& size == fingerprint.size();
		}
	}

	private record FileFingerprint(String sourceKey, long lastModifiedAt, long size) {
	}
}
