package com.authservice.app.domain.auth.sso.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AdminIpRuleSet {

	private static final Logger log = LoggerFactory.getLogger(AdminIpRuleSet.class);

	private final List<AdminIpRule> rules;

	private AdminIpRuleSet(List<AdminIpRule> rules) {
		this.rules = rules;
	}

	static AdminIpRuleSet compile(List<String> rawRules) {
		if (rawRules == null || rawRules.isEmpty()) {
			return new AdminIpRuleSet(List.of());
		}
		return new AdminIpRuleSet(rawRules.stream()
			.map(AdminIpRule::compile)
			.toList());
	}

	boolean allows(String clientIp, boolean defaultAllow) {
		if (rules.isEmpty()) {
			return defaultAllow;
		}

		InetAddress clientAddress = resolve(clientIp);
		if (clientAddress == null) {
			return false;
		}

		for (AdminIpRule rule : rules) {
			if (rule.matches(clientAddress)) {
				return true;
			}
		}
		return false;
	}

	private static InetAddress resolve(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return InetAddress.getByName(value.trim());
		} catch (UnknownHostException ex) {
			log.warn("Admin IP guard client address could not be resolved. ip={}", value, ex);
			return null;
		}
	}

	private interface AdminIpRule {
		boolean matches(InetAddress clientAddress);

		static AdminIpRule compile(String rawRule) {
			if (rawRule == null || rawRule.isBlank()) {
				return InvalidRule.INSTANCE;
			}

			String normalizedRule = rawRule.trim();
			if (normalizedRule.contains("/")) {
				return CidrRule.compile(normalizedRule);
			}

			try {
				return new SingleAddressRule(InetAddress.getByName(normalizedRule));
			} catch (UnknownHostException ex) {
				log.warn("Admin IP guard rule could not be compiled. rule={}", normalizedRule, ex);
				return InvalidRule.INSTANCE;
			}
		}
	}

	private record SingleAddressRule(InetAddress expectedAddress) implements AdminIpRule {
		@Override
		public boolean matches(InetAddress clientAddress) {
			return expectedAddress.equals(clientAddress);
		}
	}

	private record CidrRule(byte[] networkAddress, int prefixLength) implements AdminIpRule {
		private static AdminIpRule compile(String rawRule) {
			String[] parts = rawRule.split("/", 2);
			if (parts.length != 2) {
				log.warn("Admin IP guard CIDR rule is invalid. rule={}", rawRule);
				return InvalidRule.INSTANCE;
			}

			try {
				byte[] network = InetAddress.getByName(parts[0].trim()).getAddress();
				int prefix = Integer.parseInt(parts[1].trim());
				int maxPrefix = network.length * Byte.SIZE;
				if (prefix < 0 || prefix > maxPrefix) {
					log.warn("Admin IP guard CIDR prefix is invalid. rule={}", rawRule);
					return InvalidRule.INSTANCE;
				}
				return new CidrRule(network, prefix);
			} catch (UnknownHostException | IllegalArgumentException ex) {
				log.warn("Admin IP guard CIDR rule could not be compiled. rule={}", rawRule, ex);
				return InvalidRule.INSTANCE;
			}
		}

		@Override
		public boolean matches(InetAddress clientAddress) {
			byte[] client = clientAddress.getAddress();
			if (client.length != networkAddress.length) {
				return false;
			}

			int fullBytes = prefixLength / Byte.SIZE;
			int remainingBits = prefixLength % Byte.SIZE;
			for (int index = 0; index < fullBytes; index++) {
				if (client[index] != networkAddress[index]) {
					return false;
				}
			}
			if (remainingBits == 0) {
				return true;
			}

			int mask = 0xFF << (Byte.SIZE - remainingBits);
			return (client[fullBytes] & mask) == (networkAddress[fullBytes] & mask);
		}
	}

	private enum InvalidRule implements AdminIpRule {
		INSTANCE;

		@Override
		public boolean matches(InetAddress clientAddress) {
			return false;
		}
	}
}
