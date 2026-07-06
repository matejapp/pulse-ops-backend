package com.mateja.pulseops.incident.web;

import com.mateja.pulseops.incident.domain.IncidentStatus;
import com.mateja.pulseops.incident.domain.IncidentUpdate;

import java.time.Instant;
import java.util.UUID;

public record IncidentUpdateResponse(UUID incidentUpdateId, UUID incidentId,
                                     IncidentStatus status, String message, Instant createdAt) {

    // incidentId reads the LAZY incident association — call ONLY inside a @Transactional,
    // same pattern as CheckResultResponse carrying monitorId via getMonitor().
    public static IncidentUpdateResponse fromEntity(IncidentUpdate entity) {
        if (entity == null) return null;

        return new IncidentUpdateResponse(entity.getIncidentUpdateId(), entity.getIncident().getIncidentId(),
                entity.getStatus(), entity.getMessage(), entity.getCreatedAt());
    }
}
