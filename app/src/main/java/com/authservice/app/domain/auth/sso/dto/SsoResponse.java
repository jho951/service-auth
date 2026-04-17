package com.authservice.app.domain.auth.sso.dto;

import com.authservice.app.common.base.dto.BaseResponse;
import java.util.List;

public class SsoResponse {

	public static class InternalSessionValidationResponse extends BaseResponse {
		private final boolean authenticated;
		private final String userId;
		private final String role;
		private final String status;
		private final String sessionId;

		public InternalSessionValidationResponse(boolean authenticated, String userId, String role, String status, String sessionId) {
			this.authenticated = authenticated;
			this.userId = userId;
			this.role = role;
			this.status = status;
			this.sessionId = sessionId;
		}

		public boolean isAuthenticated() {
			return authenticated;
		}

		public String getUserId() {
			return userId;
		}

		public String getRole() {
			return role;
		}

		public String getStatus() {
			return status;
		}

		public String getSessionId() {
			return sessionId;
		}
	}

	public static class MeResponse extends BaseResponse {
		private final String id;
		private final String email;
		private final String name;
		private final String avatarUrl;
		private final List<String> roles;
		private final String status;

		public MeResponse(String id, String email, String name, String avatarUrl, List<String> roles, String status) {
			this.id = id;
			this.email = email;
			this.name = name;
			this.avatarUrl = avatarUrl;
			this.roles = roles;
			this.status = status;
		}

		public String getId() {
			return id;
		}

		public String getEmail() {
			return email;
		}

		public String getName() {
			return name;
		}

		public String getAvatarUrl() {
			return avatarUrl;
		}

		public List<String> getRoles() {
			return roles;
		}

		public String getStatus() {
			return status;
		}
	}
}
