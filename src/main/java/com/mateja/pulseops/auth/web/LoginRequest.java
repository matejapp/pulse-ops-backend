package com.mateja.pulseops.auth.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Input DTO for login. Deliberately looser than RegisterRequest: NO password @Size(min=...).
// Enforcing the registration min-length here would leak our password policy and could reject a
// legitimate older password if the rule ever changed. We only check presence + sane max here;
// whether the password is CORRECT is decided by AuthService against the stored hash.
public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Size(max = 255)
        @Email
        String email,
        @NotBlank(message = "Password is required")
        @Size(max = 72)
        String password

) { }
