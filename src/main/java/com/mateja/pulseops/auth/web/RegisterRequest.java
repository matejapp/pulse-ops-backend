package com.mateja.pulseops.auth.web;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;


// Input DTO for the register endpoint. A record = immutable, boilerplate-free carrier. Validation
// annotations live HERE (the boundary), not on the entity, so bad input is rejected before any logic.
public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email
        @Size(max = 255)                 // matches the DB column length (varchar(255))
        String email,
        @NotBlank(message = "Password is required")
        @Size(min = 9, max = 72)         // min: a real strength rule at REGISTER; max 72: BCrypt only hashes the first 72 bytes
        String password
) {}
