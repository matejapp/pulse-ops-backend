package com.mateja.pulseops.incident.persistence;

import com.mateja.pulseops.incident.domain.Incident;
import com.mateja.pulseops.incident.domain.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentRepo extends JpaRepository<Incident, UUID> {

    // Newest-first for the list endpoint (GET /api/incidents). No Pageable — incidents
    // are a plain list; pagination was M6's concern (check history), not this milestone.
    List<Incident> findAllByOrderByCreatedAtDesc();

    // Active (unresolved) incidents for the public status page, newest-first. join fetch pulls each
    // incident's service in the SAME query so PublicIncidentResponse.fromEntity can read the service
    // name without an N+1 across the LAZY association.
    @Query("select i from Incident i join fetch i.service where i.currentStatus <> :status order by i.createdAt desc")
    List<Incident> findActiveWithService(@Param("status") IncidentStatus status);
}
