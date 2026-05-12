package com.authservice.app.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 비밀번호 해시 인코더 빈을 제공하는 설정 클래스입니다.
 */
@Configuration
public class PasswordEncoderConfig {

	/**
	 * BCrypt 기반 비밀번호 인코더를 생성합니다.
	 *
	 * @return 애플리케이션 기본 비밀번호 인코더
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
