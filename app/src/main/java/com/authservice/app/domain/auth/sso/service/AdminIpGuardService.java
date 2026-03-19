package com.authservice.app.domain.auth.sso.service;

import com.authservice.app.common.base.constant.ErrorCode;
import com.authservice.app.common.base.exception.GlobalException;
import com.authservice.app.domain.auth.sso.config.SsoProperties;
import com.ipguard.core.engine.IpGuardEngine;
import com.ipguard.spi.RuleSource;
import jakarta.servlet.http.HttpServletRequest;
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
		RuleSource ruleSource = () -> String.join("\n", properties.parseRules());
		var decision = new IpGuardEngine(ruleSource, properties.isDefaultAllow()).decide(clientIp);

		if (!decision.allowed()) {
			log.warn("Admin IP guard blocked request. ip={}, reason={}", clientIp, decision.reason());
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
}
