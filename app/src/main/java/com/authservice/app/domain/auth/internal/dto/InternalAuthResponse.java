package com.authservice.app.domain.auth.internal.dto;

import com.authservice.app.common.base.dto.BaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class InternalAuthResponse {

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class AccountResponse extends BaseResponse {
		@Schema(description = "생성된 auth account id")
		private UUID authId;

		@Schema(description = "연결된 user id")
		private UUID userId;

		@Schema(description = "로그인 아이디")
		private String loginId;

		public static AccountResponse from(UUID authId, UUID userId, String loginId) {
			return AccountResponse.builder()
				.authId(authId)
				.userId(userId)
				.loginId(loginId)
				.build();
		}
	}
}
