package com.authservice.app.common.web;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwksController {

	private final String authJwtPublicKeyPem;
	private final String authJwtAlgorithm;
	private final String authJwtKeyId;

	private static boolean isRsaAlgorithm(String algorithm) {
		return algorithm != null && algorithm.toUpperCase().startsWith("RS");
	}

	private static RSAPublicKey parseRsaPublicKey(String pem) {
		try {
			String sanitized = pem
				.replace("-----BEGIN PUBLIC KEY-----", "")
				.replace("-----END PUBLIC KEY-----", "")
				.replaceAll("\\s+", "");
			byte[] der = Base64.getDecoder().decode(sanitized);
			X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
			return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
		} catch (Exception ex) {
			throw new IllegalArgumentException("Invalid AUTH_JWT_PUBLIC_KEY_PEM", ex);
		}
	}

	private static String toBase64UrlUnsigned(BigInteger value) {
		byte[] bytes = value.toByteArray();
		if (bytes.length > 1 && bytes[0] == 0) {
			byte[] unsigned = new byte[bytes.length - 1];
			System.arraycopy(bytes, 1, unsigned, 0, unsigned.length);
			bytes = unsigned;
		}
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public JwksController(
		@Value("${AUTH_JWT_PUBLIC_KEY_PEM:}")
		String authJwtPublicKeyPem,
		@Value("${AUTH_JWT_ALGORITHM:RS256}")
		String authJwtAlgorithm,
		@Value("${AUTH_JWT_KEY_ID:auth-jwt-key}")
		String authJwtKeyId
	) {
		this.authJwtPublicKeyPem = authJwtPublicKeyPem;
		this.authJwtAlgorithm = authJwtAlgorithm;
		this.authJwtKeyId = authJwtKeyId;
	}

	@GetMapping("/.well-known/jwks.json")
	public ResponseEntity<Map<String, Object>> jwks() {
		if (!isRsaAlgorithm(authJwtAlgorithm)) return ResponseEntity.ok(Map.of("keys", List.of()));
		if (authJwtPublicKeyPem == null ) return ResponseEntity.ok(Map.of("keys", List.of()));
		if (authJwtPublicKeyPem.isBlank()) return ResponseEntity.ok(Map.of("keys", List.of()));
		RSAPublicKey rsaPublicKey = parseRsaPublicKey(authJwtPublicKeyPem);
		Map<String, String> jwk = Map.of(
			"kty", "RSA",
			"use", "sig",
			"alg", authJwtAlgorithm,
			"kid", authJwtKeyId,
			"n", toBase64UrlUnsigned(rsaPublicKey.getModulus()),
			"e", toBase64UrlUnsigned(rsaPublicKey.getPublicExponent())
		);
		return ResponseEntity.ok(Map.of("keys", List.of(jwk)));
	}
}

