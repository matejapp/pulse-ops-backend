package com.mateja.pulseops.auth.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Size(max = 255)
        @Email
        String email,
        @NotBlank(message = "Password is required")
        @Size(max = 72)
        String password

) { }
