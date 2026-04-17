package com.authservice.app.domain.auth.dto;

import com.authservice.app.common.base.dto.BaseResponse;

/**
 * 인증 관련 응답 DTO들을 관리하는 컨테이너 클래스입니다.
 * 외부 유출을 최소화하기 위해 필요한 응답 객체들을 정적 내부 클래스로 포함합니다.
 */
public class AuthResponse {

	/** 인증 성공 후 클라이언트에게 전달할 토큰 정보를 담는 응답 객체입니다.*/
	public static class TokenResponse extends BaseResponse {
		private String accessToken;
		private String refreshToken;

		/** JSON 역직렬화(Deserialization)를 위한 기본 생성자입니다. */
		public TokenResponse() {}

		/**
		 * 생성자
		 *
		 * @param accessToken  서비스 접근을 위한 액세스 토큰
		 * @param refreshToken 토큰 갱신을 위한 리프레시 토큰
		 */
		public TokenResponse(String accessToken, String refreshToken) {
			this.accessToken = accessToken;
			this.refreshToken = refreshToken;
		}

		public String getAccessToken() {return accessToken;}
		public String getRefreshToken() {return refreshToken;}

		/**
		 * 빌더 객체를 생성합니다.
		 * @return TokenResponseBuilder
		 */
		public static TokenResponseBuilder builder() {
			return new TokenResponseBuilder();
		}

		/** TokenResponse 생성을 위한 내부 빌더 클래스입니다. */
		public static class TokenResponseBuilder {
			private String accessToken;
			private String refreshToken;

			TokenResponseBuilder() {}

			public TokenResponseBuilder accessToken(String accessToken) {
				this.accessToken = accessToken;
				return this;
			}

			public TokenResponseBuilder refreshToken(String refreshToken) {
				this.refreshToken = refreshToken;
				return this;
			}

			public TokenResponse build() {
				return new TokenResponse(accessToken, refreshToken);
			}
		}
	}
}
