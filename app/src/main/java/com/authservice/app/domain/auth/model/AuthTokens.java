package com.authservice.app.domain.auth.model;

public record AuthTokens(String accessToken, String refreshToken) {
}
