package com.mateja.pulseops.auth.web;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;


public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email
        @Size(max = 255)
        String email,
        @NotBlank(message = "Password is required")
        @Size(min = 9, max = 72)
        String password
) {}
