package com.mateja.pulseops.incident.application;

import com.mateja.pulseops.incident.domain.Incident;
import com.mateja.pulseops.incident.domain.IncidentStatus;
import com.mateja.pulseops.incident.domain.IncidentUpdate;
import com.mateja.pulseops.incident.persistence.IncidentRepo;
import com.mateja.pulseops.incident.persistence.IncidentUpdateRepo;
import com.mateja.pulseops.incident.web.AddIncidentUpdateRequest;
import com.mateja.pulseops.incident.web.IncidentDetailResponse;
import com.mateja.pulseops.incident.web.IncidentResponse;
import com.mateja.pulseops.incident.web.OpenIncidentRequest;
import com.mateja.pulseops.monitoredservice.application.MonitoredServiceNotFoundException;
import com.mateja.pulseops.monitoredservice.domain.MonitoredService;
import com.mateja.pulseops.monitoredservice.persistence.MonitoredServiceRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class IncidentService {

    private final IncidentRepo incidentRepo;
    private final IncidentUpdateRepo incidentUpdateRepo;
    private final MonitoredServiceRepo monitoredServiceRepo;

    public IncidentService(IncidentRepo incidentRepo, IncidentUpdateRepo incidentUpdateRepo,
                           MonitoredServiceRepo monitoredServiceRepo) {
        this.incidentRepo = incidentRepo;
        this.incidentUpdateRepo = incidentUpdateRepo;
        this.monitoredServiceRepo = monitoredServiceRepo;
    }

    // Open a new incident. The incident row AND its first timeline update are written in the SAME
    // transaction, so an incident is never persisted with an empty timeline. Birth status is
    // INVESTIGATING (forced by the Incident constructor), so the opening update records that state.
    @Transactional
    public IncidentResponse openIncident(OpenIncidentRequest request) {
        MonitoredService service = monitoredServiceRepo.findById(request.serviceId())
                .orElseThrow(() -> new MonitoredServiceNotFoundException("Service not found"));

        Incident incident = new Incident(service, request.title());
        // saveAndFlush (not save) so the INSERT runs now and the @CreationTimestamp createdAt is
        // populated before we map the response — otherwise fromEntity would serialize createdAt as null
        // (flush would only happen at commit). The opening update can use a plain save (its timestamp
        // isn't in this response). Matches MonitoredServiceService.createMonitoredService.
        incidentRepo.saveAndFlush(incident);

        IncidentUpdate opening = new IncidentUpdate(incident, incident.getCurrentStatus(), request.message());
        incidentUpdateRepo.save(opening);

        return IncidentResponse.fromEntity(incident);
    }

    // Add a timeline update, possibly transitioning the incident's status. transitionTo enforces the
    // forward-only + terminal-lock rules (throws InvalidIncidentTransitionException -> 409). The dirty
    // current_status / resolved_at fields flush automatically at commit; no explicit incident save.
    @Transactional
    public IncidentDetailResponse addUpdate(UUID incidentId, AddIncidentUpdateRequest request) {
        Incident incident = incidentRepo.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException("Incident not found"));

        incident.transitionTo(request.status());

        IncidentUpdate update = new IncidentUpdate(incident, request.status(), request.message());
        incidentUpdateRepo.save(update);

        List<IncidentUpdate> timeline = incidentUpdateRepo.findByIncident_IncidentIdOrderByCreatedAtAsc(incidentId);
        return IncidentDetailResponse.fromEntity(incident, timeline);
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> getAll() {
        return incidentRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(IncidentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public IncidentDetailResponse getOne(UUID incidentId) {
        Incident incident = incidentRepo.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException("Incident not found"));

        List<IncidentUpdate> timeline = incidentUpdateRepo.findByIncident_IncidentIdOrderByCreatedAtAsc(incidentId);
        return IncidentDetailResponse.fromEntity(incident, timeline);
    }
}
