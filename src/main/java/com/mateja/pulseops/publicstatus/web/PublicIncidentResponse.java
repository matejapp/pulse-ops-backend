package com.mateja.pulseops.publicstatus.web;

import com.mateja.pulseops.incident.domain.Incident;
import com.mateja.pulseops.incident.domain.IncidentStatus;

import java.time.Instant;
import java.util.UUID;

// Public view of an incident. Carries the service NAME (not id) — the public page has no notion of
// internal service ids. fromEntity reads the LAZY service association, so call it inside a
// @Transactional (or via a join-fetched incident).
public record PublicIncidentResponse(UUID incidentId, String serviceName, String title,
                                     IncidentStatus currentStatus, Instant createdAt, Instant resolvedAt) {

    public static PublicIncidentResponse fromEntity(Incident entity) {
        if (entity == null) return null;

        return new PublicIncidentResponse(entity.getIncidentId(), entity.getService().getName(),
                entity.getTitle(), entity.getCurrentStatus(), entity.getCreatedAt(), entity.getResolvedAt());
    }
}
