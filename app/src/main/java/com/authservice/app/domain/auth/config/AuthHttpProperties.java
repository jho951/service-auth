package com.authservice.app.domain.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public class AuthHttpProperties {

	private String bearerPrefix = "Bearer ";
	private boolean refreshCookieEnabled = true;
	private String refreshCookieName = "refresh_token";
	private boolean refreshCookieHttpOnly = true;
	private boolean refreshCookieSecure;
	private String refreshCookiePath = "/";
	private String refreshCookieSameSite = "Lax";
	private OAuth2 oauth2 = new OAuth2();
	private Jwt jwt = new Jwt();

	public String getBearerPrefix() {
		return bearerPrefix;
	}

	public void setBearerPrefix(String bearerPrefix) {
		this.bearerPrefix = bearerPrefix;
	}

	public boolean isRefreshCookieEnabled() {
		return refreshCookieEnabled;
	}

	public void setRefreshCookieEnabled(boolean refreshCookieEnabled) {
		this.refreshCookieEnabled = refreshCookieEnabled;
	}

	public String getRefreshCookieName() {
		return refreshCookieName;
	}

	public void setRefreshCookieName(String refreshCookieName) {
		this.refreshCookieName = refreshCookieName;
	}

	public boolean isRefreshCookieHttpOnly() {
		return refreshCookieHttpOnly;
	}

	public void setRefreshCookieHttpOnly(boolean refreshCookieHttpOnly) {
		this.refreshCookieHttpOnly = refreshCookieHttpOnly;
	}

	public boolean isRefreshCookieSecure() {
		return refreshCookieSecure;
	}

	public void setRefreshCookieSecure(boolean refreshCookieSecure) {
		this.refreshCookieSecure = refreshCookieSecure;
	}

	public String getRefreshCookiePath() {
		return refreshCookiePath;
	}

	public void setRefreshCookiePath(String refreshCookiePath) {
		this.refreshCookiePath = refreshCookiePath;
	}

	public String getRefreshCookieSameSite() {
		return refreshCookieSameSite;
	}

	public void setRefreshCookieSameSite(String refreshCookieSameSite) {
		this.refreshCookieSameSite = refreshCookieSameSite;
	}

	public OAuth2 getOauth2() {
		return oauth2;
	}

	public void setOauth2(OAuth2 oauth2) {
		this.oauth2 = oauth2 == null ? new OAuth2() : oauth2;
	}

	public Jwt getJwt() {
		return jwt;
	}

	public void setJwt(Jwt jwt) {
		this.jwt = jwt == null ? new Jwt() : jwt;
	}

	public static class OAuth2 {
		private String authorizationBaseUri = "/oauth2/authorization";

		public String getAuthorizationBaseUri() {
			return authorizationBaseUri;
		}

		public void setAuthorizationBaseUri(String authorizationBaseUri) {
			this.authorizationBaseUri = authorizationBaseUri;
		}
	}

	public static class Jwt {
		private String secret = "";
		private long accessSeconds = 1200L;
		private long refreshSeconds = 30000L;

		public String getSecret() {
			return secret;
		}

		public void setSecret(String secret) {
			this.secret = secret == null ? "" : secret;
		}

		public long getAccessSeconds() {
			return accessSeconds;
		}

		public void setAccessSeconds(long accessSeconds) {
			this.accessSeconds = accessSeconds;
		}

		public long getRefreshSeconds() {
			return refreshSeconds;
		}

		public void setRefreshSeconds(long refreshSeconds) {
			this.refreshSeconds = refreshSeconds;
		}
	}
}
