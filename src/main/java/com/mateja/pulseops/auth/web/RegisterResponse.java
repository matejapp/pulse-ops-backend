package com.mateja.pulseops.auth.web;

import com.mateja.pulseops.auth.domain.Role;

import java.time.Instant;
import java.util.UUID;

public record RegisterResponse (
        UUID userId,
        String email,
        Role role,
        Instant createdAt
) {}

