package com.mateja.pulseops.incident.persistence;

import com.mateja.pulseops.incident.domain.IncidentUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentUpdateRepo extends JpaRepository<IncidentUpdate, UUID> {

    // Chronological timeline for one incident. Nested-property traversal (Incident_IncidentId)
    // + ascending order hits the V5 index on (incident_id, created_at).
    List<IncidentUpdate> findByIncident_IncidentIdOrderByCreatedAtAsc(UUID incidentId);
}
