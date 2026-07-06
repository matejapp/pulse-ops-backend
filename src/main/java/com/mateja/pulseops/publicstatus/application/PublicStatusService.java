package com.mateja.pulseops.publicstatus.application;

import com.mateja.pulseops.httpmonitor.domain.MonitorStatus;
import com.mateja.pulseops.httpmonitor.persistence.HttpMonitorRepo;
import com.mateja.pulseops.httpmonitor.persistence.ServiceMonitorStatusView;
import com.mateja.pulseops.incident.application.IncidentNotFoundException;
import com.mateja.pulseops.incident.domain.Incident;
import com.mateja.pulseops.incident.domain.IncidentStatus;
import com.mateja.pulseops.incident.domain.IncidentUpdate;
import com.mateja.pulseops.incident.persistence.IncidentRepo;
import com.mateja.pulseops.incident.persistence.IncidentUpdateRepo;
import com.mateja.pulseops.monitoredservice.domain.MonitoredService;
import com.mateja.pulseops.monitoredservice.persistence.MonitoredServiceRepo;
import com.mateja.pulseops.publicstatus.domain.ServiceStatus;
import com.mateja.pulseops.publicstatus.web.PublicIncidentDetailResponse;
import com.mateja.pulseops.publicstatus.web.PublicIncidentResponse;
import com.mateja.pulseops.publicstatus.web.PublicServiceStatus;
import com.mateja.pulseops.publicstatus.web.PublicStatusResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PublicStatusService {

    private final MonitoredServiceRepo monitoredServiceRepo;
    private final HttpMonitorRepo httpMonitorRepo;
    private final IncidentRepo incidentRepo;
    private final IncidentUpdateRepo incidentUpdateRepo;

    public PublicStatusService(MonitoredServiceRepo monitoredServiceRepo, HttpMonitorRepo httpMonitorRepo,
                               IncidentRepo incidentRepo, IncidentUpdateRepo incidentUpdateRepo) {
        this.monitoredServiceRepo = monitoredServiceRepo;
        this.httpMonitorRepo = httpMonitorRepo;
        this.incidentRepo = incidentRepo;
        this.incidentUpdateRepo = incidentUpdateRepo;
    }

    // Build the status page: derive each service's status from its monitors, then the overall banner
    // from the per-service statuses. Two queries total (all services + all monitor statuses); the
    // grouping happens in memory, so there is no per-service query and no LAZY walk.
    @Transactional(readOnly = true)
    public PublicStatusResponse getStatusPage() {
        List<MonitoredService> services = monitoredServiceRepo.findAll();

        Map<UUID, List<MonitorStatus>> statusesByService = httpMonitorRepo.findServiceMonitorStatuses().stream()
                .collect(Collectors.groupingBy(
                        ServiceMonitorStatusView::getServiceId,
                        Collectors.mapping(ServiceMonitorStatusView::getStatus, Collectors.toList())));

        List<PublicServiceStatus> serviceStatuses = services.stream()
                .map(service -> new PublicServiceStatus(
                        service.getServiceId(),
                        service.getName(),
                        // getOrDefault(empty) -> a service with no monitors derives to UNCHECKED.
                        ServiceStatus.fromMonitors(statusesByService.getOrDefault(service.getServiceId(), List.of()))))
                .toList();

        ServiceStatus overall = ServiceStatus.worstOf(
                serviceStatuses.stream().map(PublicServiceStatus::status).toList());

        return new PublicStatusResponse(overall, serviceStatuses);
    }

    @Transactional(readOnly = true)
    public List<PublicIncidentResponse> getActiveIncidents() {
        return incidentRepo.findActiveWithService(IncidentStatus.RESOLVED).stream()
                .map(PublicIncidentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicIncidentDetailResponse getIncident(UUID incidentId) {
        Incident incident = incidentRepo.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException("Incident not found"));

        List<IncidentUpdate> timeline = incidentUpdateRepo.findByIncident_IncidentIdOrderByCreatedAtAsc(incidentId);
        return PublicIncidentDetailResponse.fromEntity(incident, timeline);
    }
}
