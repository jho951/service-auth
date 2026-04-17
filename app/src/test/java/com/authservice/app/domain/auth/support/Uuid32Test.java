package com.authservice.app.domain.auth.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class Uuid32Test {

	@Test
	void convertsUuidToUuid32AndBack() {
		UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

		String uuid32 = Uuid32.fromUuid(uuid);

		assertThat(uuid32).isEqualTo("550e8400e29b41d4a716446655440000");
		assertThat(Uuid32.toUuid(uuid32)).isEqualTo(uuid);
	}

	@Test
	void acceptsCanonicalUuidStringForCompatibility() {
		UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

		assertThat(Uuid32.toUuid("550e8400-e29b-41d4-a716-446655440000")).isEqualTo(uuid);
	}
}
