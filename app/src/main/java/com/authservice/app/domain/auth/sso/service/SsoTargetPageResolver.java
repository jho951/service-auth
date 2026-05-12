package com.authservice.app.domain.auth.sso.service;

import com.authservice.app.domain.auth.sso.config.SsoProperties;
import com.authservice.app.domain.auth.sso.model.SsoPageType;
import com.authservice.app.domain.auth.sso.model.SsoTargetPage;
import com.authservice.common.base.constant.ErrorCode;
import com.authservice.common.base.exception.GlobalException;
import java.net.URI;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SsoTargetPageResolver {

	private final SsoProperties properties;

	public SsoTargetPageResolver(SsoProperties properties) {
		this.properties = properties;
	}

	public SsoTargetPage resolve(String page, String redirectUri) {
		if (page != null && !page.isBlank()) {
			SsoPageType pageType = SsoPageType.from(page);
			String configuredRedirectUri = configuredRedirectUri(pageType);
			if (redirectUri != null && !redirectUri.isBlank()) {
				validateRedirectUri(redirectUri, configuredRedirectUri);
			}
			return new SsoTargetPage(pageType, configuredRedirectUri);
		}

		if (redirectUri == null || redirectUri.isBlank()) {
			throw new GlobalException(ErrorCode.INVALID_REQUEST);
		}

		String normalized = normalizeRedirectUri(redirectUri);
		for (SsoPageType pageType : SsoPageType.values()) {
			String configuredRedirectUri = configuredRedirectUri(pageType);
			if (normalizeRedirectUri(configuredRedirectUri).equals(normalized)) {
				return new SsoTargetPage(pageType, configuredRedirectUri);
			}
		}

		throw new GlobalException(ErrorCode.INVALID_REQUEST);
	}

	public URI successRedirect(String redirectUri, String ticket) {
		return URI.create(
			UriComponentsBuilder.fromUriString(redirectUri)
				.queryParam("ticket", ticket)
				.build(true)
				.toUriString()
		);
	}

	public URI failureRedirect(String redirectUri) {
		return URI.create(
			UriComponentsBuilder.fromUriString(redirectUri)
				.queryParam("error", "oauth_failed")
				.build(true)
				.toUriString()
		);
	}

	private void validateRedirectUri(String redirectUri, String expectedRedirectUri) {
		if (!normalizeRedirectUri(redirectUri).equals(normalizeRedirectUri(expectedRedirectUri))) {
			throw new GlobalException(ErrorCode.INVALID_REQUEST);
		}
	}

	private String configuredRedirectUri(SsoPageType pageType) {
		return switch (pageType) {
			case EXPLAIN -> properties.getFrontend().getExplain().getRedirectUri();
			case EDITOR -> properties.getFrontend().getEditor().getRedirectUri();
			case ADMIN -> properties.getFrontend().getAdmin().getRedirectUri();
		};
	}

	private String normalizeRedirectUri(String redirectUri) {
		URI requested = URI.create(redirectUri);
		String path = requested.getPath();
		if (path == null || path.isBlank()) {
			path = "/";
		}

		return UriComponentsBuilder.newInstance()
			.scheme(requested.getScheme())
			.host(requested.getHost())
			.port(requested.getPort())
			.path(path)
			.build()
			.toUriString();
	}
}
