package com.authservice.app.domain.auth.service;

import com.authservice.app.domain.auth.entity.Auth;
import com.authservice.app.domain.auth.entity.AuthAuditLog;
import com.authservice.app.domain.auth.repository.AuthAuditLogRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthAuditService {

	private final AuthAuditLogRepository authAuditLogRepository;

	public AuthAuditService(AuthAuditLogRepository authAuditLogRepository) {
		this.authAuditLogRepository = authAuditLogRepository;
	}

	@Transactional
	public void log(String eventType, String result, Auth auth, AuthRequestContext context, String metadataJson) {
		authAuditLogRepository.save(new AuthAuditLog(
			auth == null ? null : auth.getUserId(),
			auth == null ? null : auth.getId(),
			eventType,
			context == null ? "" : context.ip(),
			context == null ? "" : context.userAgent(),
			result,
			metadataJson
		));
	}

	@Transactional
	public void log(String eventType, String result, UUID userId, AuthRequestContext context, String metadataJson) {
		authAuditLogRepository.save(new AuthAuditLog(
			userId,
			null,
			eventType,
			context == null ? "" : context.ip(),
			context == null ? "" : context.userAgent(),
			result,
			metadataJson
		));
	}
}
