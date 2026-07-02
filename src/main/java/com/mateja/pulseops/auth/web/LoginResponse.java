package com.mateja.pulseops.auth.web;

public record LoginResponse (
        String accessToken,
        String tokenType,
        long expiresIn
) {}
