package com.authservice.app;

import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(properties = {
	"SPRING_PROFILES_ACTIVE=dev",
	"AUTH_JWT_SECRET=test-auth-jwt-secret-test-auth-jwt-secret"
})
class AuthServiceApplicationTests {
}
