package com.mateja.pulseops.incident.web;

import com.mateja.pulseops.incident.domain.Incident;
import com.mateja.pulseops.incident.domain.IncidentUpdate;

import java.util.List;

// One incident plus its full timeline. Returned by GET /api/incidents/{incidentId}.
// The list endpoint returns bare IncidentResponse rows (no timeline) to avoid an N+1.
public record IncidentDetailResponse(IncidentResponse incident, List<IncidentUpdateResponse> updates) {

    public static IncidentDetailResponse fromEntity(Incident incident, List<IncidentUpdate> updates) {
        return new IncidentDetailResponse(
                IncidentResponse.fromEntity(incident),
                updates.stream().map(IncidentUpdateResponse::fromEntity).toList()
        );
    }
}
