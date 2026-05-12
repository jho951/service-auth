package com.authservice.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.authservice.app.domain.auth.internal.controller.InternalSessionController;
import com.authservice.app.domain.auth.sso.dto.SsoResponse;
import com.authservice.app.domain.auth.sso.service.SsoInternalSessionValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class InternalSessionControllerFlowTest {

	@Mock
	private SsoInternalSessionValidationService ssoInternalSessionValidationService;

	@Test
	void validateSessionDelegatesToInternalSessionValidationService() {
		InternalSessionController controller = new InternalSessionController(ssoInternalSessionValidationService);
		MockHttpServletRequest request = new MockHttpServletRequest();
		SsoResponse.InternalSessionValidationResponse validation =
			new SsoResponse.InternalSessionValidationResponse(false, "", "", "", "");
		when(ssoInternalSessionValidationService.validate(request)).thenReturn(ResponseEntity.status(401).body(validation));

		ResponseEntity<SsoResponse.InternalSessionValidationResponse> response = controller.validateSession(request);

		assertThat(response.getStatusCode().value()).isEqualTo(401);
		assertThat(response.getBody()).isSameAs(validation);
		verify(ssoInternalSessionValidationService).validate(request);
	}
}
