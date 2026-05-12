package com.authservice.app.domain.auth.sso.service;

import com.authservice.common.base.constant.ErrorCode;
import com.authservice.common.base.exception.GlobalException;
import com.authservice.app.domain.auth.sso.config.SsoProperties;
import com.authservice.common.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AdminIpGuardService {

	private static final Logger log = LoggerFactory.getLogger(AdminIpGuardService.class);

	private final SsoProperties ssoProperties;
	private final AdminIpRuleProvider adminIpRuleProvider;

	public AdminIpGuardService(SsoProperties ssoProperties, AdminIpRuleProvider adminIpRuleProvider) {
		this.ssoProperties = ssoProperties;
		this.adminIpRuleProvider = adminIpRuleProvider;
	}

	public void validate(HttpServletRequest request) {
		SsoProperties.AdminIpGuard properties = ssoProperties.getFrontend().getAdmin().getIpGuard();
		if (!properties.isEnabled()) {
			return;
		}

		String clientIp = ClientIpResolver.resolve(request);
		if (!adminIpRuleProvider.currentRuleSet(properties).allows(clientIp, properties.isDefaultAllow())) {
			log.warn("Admin IP guard blocked request. ip={}", clientIp);
			throw new GlobalException(ErrorCode.FORBIDDEN);
		}
	}
}
