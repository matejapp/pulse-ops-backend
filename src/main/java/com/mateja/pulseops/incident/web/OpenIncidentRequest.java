package com.mateja.pulseops.incident.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record OpenIncidentRequest(
        @NotNull(message = "Service id is required!")
        UUID serviceId,
        @NotBlank(message = "Title is required!")
        @Size(max = 200)
        String title,
        // The opening timeline note. Required — an incident's first update should carry context.
        @NotBlank(message = "Message is required!")
        @Size(max = 5000)
        String message
) {
}
