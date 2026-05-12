package com.authservice.common.logging;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** 사용자의 민감한 정보(개인정보)가 그대로 노출되지 않도록 별표(***) 처리 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SensitiveDataMasker {

	/**
	 * 이메일 형식(@ 포함): 첫 글자만 남기고 골뱅이(@) 앞까지 마스킹 예시) haehak@gmail.com → h***@gmail.com
	 * 짧은 문자열(2자 이하): 전체를 별표 처리 예시) it → ***
	 * 일반 문자열 (3자 이상): 첫 글자와 마지막 글자만 남기고 마스킹 예시) justdoit → j***t
	 * 데이터 없음 (null / 공백): unknown 반환 예시) null → unknown
	 * @param value 입력된 문자열
	 * @return 마스킹 된 문자열
	 */
	public static String maskIdentifier(String value) {
		if (value == null) return "unknown";
		if (value.isBlank()) return "unknown";

		String trimmed = value.trim();
		int atIndex = trimmed.indexOf('@');

		if (atIndex > 1) return trimmed.charAt(0) + "***" + trimmed.substring(atIndex);
		if (trimmed.length() <= 2) return "***";
		return trimmed.charAt(0) + "***" + trimmed.charAt(trimmed.length() - 1);
	}
}
