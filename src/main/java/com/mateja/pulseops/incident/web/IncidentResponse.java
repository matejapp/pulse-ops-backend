package com.mateja.pulseops.incident.web;

import com.mateja.pulseops.incident.domain.Incident;
import com.mateja.pulseops.incident.domain.IncidentStatus;

import java.time.Instant;
import java.util.UUID;

public record IncidentResponse(UUID incidentId, UUID serviceId, String title,
                               IncidentStatus currentStatus, Instant createdAt, Instant resolvedAt) {

    // serviceId reads the LAZY service association — call ONLY inside the service's @Transactional,
    // same rule as HttpMonitorResponse.fromEntity touching getMonitoredService().
    public static IncidentResponse fromEntity(Incident entity) {
        if (entity == null) return null;

        return new IncidentResponse(entity.getIncidentId(), entity.getService().getServiceId(),
                entity.getTitle(), entity.getCurrentStatus(), entity.getCreatedAt(), entity.getResolvedAt());
    }
}
