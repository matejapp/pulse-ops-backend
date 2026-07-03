package com.mateja.pulseops.monitoredservice.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateServiceRequest (
        @NotBlank(message = "Name is required!")
        @Size(min = 3, max = 100)
        String name,
        @Size(max = 1000)
        String description
) {}
