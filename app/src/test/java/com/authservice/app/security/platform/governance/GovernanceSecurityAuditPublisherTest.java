package com.authservice.app.security.platform.governance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.jho951.platform.governance.api.AuditEntry;
import io.github.jho951.platform.governance.api.GovernanceAuditRecorder;
import io.github.jho951.platform.security.api.SecurityAuditEvent;
import io.github.jho951.platform.security.api.SecurityAuditMode;
import io.github.jho951.platform.security.api.SecurityDecision;
import io.github.jho951.platform.security.governance.GovernanceSecurityAuditPublisher;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GovernanceSecurityAuditPublisherTest {

	@Test
	void publishesSecurityAuditEventToGovernanceAuditRecorder() {
		GovernanceAuditRecorder auditLogRecorder = mock(GovernanceAuditRecorder.class);
		GovernanceSecurityAuditPublisher publisher = new GovernanceSecurityAuditPublisher(
			auditLogRecorder,
			SecurityAuditMode.ALL
		);
		Instant occurredAt = Instant.parse("2026-04-19T00:00:00Z");

		publisher.publish(new SecurityAuditEvent(
			false,
			SecurityDecision.DENY,
			"boundary-ip",
			"admin ip denied",
			"ADMIN",
			"BROWSER",
			"HYBRID",
			"/admin/users",
			"GET",
			"203.0.113.10",
			"admin-user",
			occurredAt
		));

		ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
		verify(auditLogRecorder).record(captor.capture());
		AuditEntry entry = captor.getValue();

		assertThat(entry.category()).isEqualTo("security");
		assertThat(entry.message()).isEqualTo("security evaluated");
		assertThat(entry.occurredAt()).isEqualTo(occurredAt);
		assertThat(entry.attributes())
			.containsEntry("security.allowed", "false")
			.containsEntry("security.decision", "DENY")
			.containsEntry("security.policy", "boundary-ip")
			.containsEntry("security.reason", "admin ip denied")
			.containsEntry("security.boundary", "ADMIN")
			.containsEntry("security.client-type", "BROWSER")
			.containsEntry("security.auth-mode", "HYBRID")
			.containsEntry("security.path", "/admin/users")
			.containsEntry("security.action", "GET")
			.containsEntry("security.client-ip", "203.0.113.10")
			.containsEntry("security.principal", "admin-user");
	}
}
