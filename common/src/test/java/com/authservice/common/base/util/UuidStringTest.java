package com.authservice.common.base.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidStringTest {

	@Test
	void storesUuidAsCanonicalChar36String() {
		UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

		String value = UuidString.fromUuid(uuid);

		assertThat(value).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
		assertThat(value).hasSize(36);
		assertThat(UuidString.toUuid(value)).isEqualTo(uuid);
	}

	@Test
	void rejectsHyphenlessUuidString() {
		assertThatThrownBy(() -> UuidString.toUuid("550e8400e29b41d4a716446655440000"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("UUID must be canonical CHAR(36).");
	}
}
