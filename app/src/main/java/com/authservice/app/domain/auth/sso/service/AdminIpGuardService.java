package com.authservice.app.domain.auth.sso.service;

import com.authservice.common.base.constant.ErrorCode;
import com.authservice.common.base.exception.GlobalException;
import com.authservice.app.domain.auth.sso.config.SsoProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AdminIpGuardService {

	private static final Logger log = LoggerFactory.getLogger(AdminIpGuardService.class);

	private final SsoProperties ssoProperties;

	public AdminIpGuardService(SsoProperties ssoProperties) {
		this.ssoProperties = ssoProperties;
	}

	public void validate(HttpServletRequest request) {
		SsoProperties.AdminIpGuard properties = ssoProperties.getFrontend().getAdmin().getIpGuard();
		if (!properties.isEnabled()) {
			return;
		}

		String clientIp = extractClientIp(request);
		if (!isAllowed(clientIp, resolveRules(properties), properties.isDefaultAllow())) {
			log.warn("Admin IP guard blocked request. ip={}", clientIp);
			throw new GlobalException(ErrorCode.FORBIDDEN);
		}
	}

	private String extractClientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (xff != null && !xff.isBlank()) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private List<String> resolveRules(SsoProperties.AdminIpGuard properties) {
		String rulesFile = properties.getRulesFile();
		if (rulesFile == null || rulesFile.isBlank()) {
			return properties.parseRules();
		}

		try {
			return Files.readString(Path.of(rulesFile.trim()), StandardCharsets.UTF_8)
				.lines()
				.map(String::trim)
				.filter(value -> !value.isBlank())
				.toList();
		} catch (IOException ex) {
			log.warn("Admin IP guard rules file could not be read. path={}", rulesFile, ex);
			throw new GlobalException(ErrorCode.FORBIDDEN);
		}
	}

	private boolean isAllowed(String clientIp, List<String> rules, boolean defaultAllow) {
		if (rules == null || rules.isEmpty()) {
			return defaultAllow;
		}

		for (String rule : rules) {
			if (matches(clientIp, rule)) {
				return true;
			}
		}
		return false;
	}

	private boolean matches(String clientIp, String rule) {
		if (clientIp == null || clientIp.isBlank() || rule == null || rule.isBlank()) {
			return false;
		}
		try {
			if (rule.contains("/")) {
				return matchesCidr(clientIp, rule);
			}
			return InetAddress.getByName(clientIp).equals(InetAddress.getByName(rule.trim()));
		} catch (UnknownHostException | IllegalArgumentException ex) {
			log.warn("Admin IP guard rule could not be evaluated. ip={}, rule={}", clientIp, rule, ex);
			return false;
		}
	}

	private boolean matchesCidr(String clientIp, String cidrRule) throws UnknownHostException {
		String[] parts = cidrRule.trim().split("/", 2);
		if (parts.length != 2) {
			return false;
		}

		byte[] client = InetAddress.getByName(clientIp).getAddress();
		byte[] network = InetAddress.getByName(parts[0].trim()).getAddress();
		if (client.length != network.length) {
			return false;
		}

		int prefix = Integer.parseInt(parts[1].trim());
		int maxPrefix = client.length * Byte.SIZE;
		if (prefix < 0 || prefix > maxPrefix) {
			return false;
		}

		int fullBytes = prefix / Byte.SIZE;
		int remainingBits = prefix % Byte.SIZE;
		for (int index = 0; index < fullBytes; index++) {
			if (client[index] != network[index]) {
				return false;
			}
		}
		if (remainingBits == 0) {
			return true;
		}
		int mask = 0xFF << (Byte.SIZE - remainingBits);
		return (client[fullBytes] & mask) == (network[fullBytes] & mask);
	}
}
