package com.authservice.app.security.platform.issuer;

import com.authservice.app.domain.auth.model.AuthPrincipal;
import com.authservice.app.domain.auth.model.AuthTokens;
import io.github.jho951.platform.security.auth.PlatformAuthenticatedPrincipal;
import io.github.jho951.platform.security.auth.PlatformIssuedToken;
import io.github.jho951.platform.security.auth.TokenIssuanceCapability;
import java.util.LinkedHashSet;
import org.springframework.stereotype.Component;

/**
 * auth-service 도메인 principal을 platform-security 발급 capability에 연결합니다.
 */
@Component
public class PlatformAuthTokenIssuer {

	private final TokenIssuanceCapability tokenIssuanceCapability;

	public PlatformAuthTokenIssuer(TokenIssuanceCapability tokenIssuanceCapability) {
		this.tokenIssuanceCapability = tokenIssuanceCapability;
	}

	public AuthTokens issue(AuthPrincipal principal) {
		PlatformIssuedToken issuedToken = tokenIssuanceCapability.issue(toPlatformPrincipal(principal));
		return new AuthTokens(issuedToken.accessToken(), issuedToken.refreshToken());
	}

	private static PlatformAuthenticatedPrincipal toPlatformPrincipal(AuthPrincipal principal) {
		return new PlatformAuthenticatedPrincipal(
			principal.userId(),
			new LinkedHashSet<>(principal.roles()),
			principal.attributes()
		);
	}
}
