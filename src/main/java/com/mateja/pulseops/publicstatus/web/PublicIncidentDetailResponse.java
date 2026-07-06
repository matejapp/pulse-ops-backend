package com.mateja.pulseops.publicstatus.web;

import com.mateja.pulseops.incident.domain.Incident;
import com.mateja.pulseops.incident.domain.IncidentUpdate;
import com.mateja.pulseops.incident.web.IncidentUpdateResponse;

import java.util.List;

// Public incident detail: the incident plus its full timeline. Reuses IncidentUpdateResponse for the
// timeline rows (an update carries no sensitive fields). Returned by GET /api/public/incidents/{id}.
public record PublicIncidentDetailResponse(PublicIncidentResponse incident, List<IncidentUpdateResponse> updates) {

    public static PublicIncidentDetailResponse fromEntity(Incident incident, List<IncidentUpdate> updates) {
        return new PublicIncidentDetailResponse(
                PublicIncidentResponse.fromEntity(incident),
                updates.stream().map(IncidentUpdateResponse::fromEntity).toList()
        );
    }
}
