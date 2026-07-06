package com.mateja.pulseops.incident.web;

import com.mateja.pulseops.incident.domain.IncidentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddIncidentUpdateRequest(
        // @NotNull, not @NotBlank: status is an enum. Jackson maps "INVESTIGATING"/"MONITORING"/
        // "RESOLVED" to the enum; an omitted or unknown value is rejected instead of persisting null.
        @NotNull(message = "Status is required!")
        IncidentStatus status,
        @NotBlank(message = "Message is required!")
        @Size(max = 5000)
        String message
) {
}
