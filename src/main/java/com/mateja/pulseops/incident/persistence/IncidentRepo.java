package com.mateja.pulseops.incident.persistence;

import com.mateja.pulseops.incident.domain.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentRepo extends JpaRepository<Incident, UUID> {

    // Newest-first for the list endpoint (GET /api/incidents). No Pageable — incidents
    // are a plain list; pagination was M6's concern (check history), not this milestone.
    List<Incident> findAllByOrderByCreatedAtDesc();
}
