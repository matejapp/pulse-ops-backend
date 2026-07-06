package com.mateja.pulseops.publicstatus.application;

import com.mateja.pulseops.httpmonitor.domain.MonitorStatus;
import com.mateja.pulseops.httpmonitor.persistence.HttpMonitorRepo;
import com.mateja.pulseops.httpmonitor.persistence.ServiceMonitorStatusView;
import com.mateja.pulseops.incident.application.IncidentNotFoundException;
import com.mateja.pulseops.incident.persistence.IncidentRepo;
import com.mateja.pulseops.incident.persistence.IncidentUpdateRepo;
import com.mateja.pulseops.monitoredservice.domain.MonitoredService;
import com.mateja.pulseops.monitoredservice.persistence.MonitoredServiceRepo;
import com.mateja.pulseops.publicstatus.domain.ServiceStatus;
import com.mateja.pulseops.publicstatus.web.PublicServiceStatus;
import com.mateja.pulseops.publicstatus.web.PublicStatusResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

// Unit test for the aggregation WIRING: grouping monitor statuses by service, deriving each service's
// status, and rolling up the overall banner. Repos are mocked; the pure precedence rules themselves
// are covered by ServiceStatusTest.
@ExtendWith(MockitoExtension.class)
class PublicStatusServiceTest {

    @Mock private MonitoredServiceRepo monitoredServiceRepo;
    @Mock private HttpMonitorRepo httpMonitorRepo;
    @Mock private IncidentRepo incidentRepo;
    @Mock private IncidentUpdateRepo incidentUpdateRepo;

    @InjectMocks private PublicStatusService service;

    // Minimal implementation of the interface projection returned by findServiceMonitorStatuses().
    private record Row(UUID serviceId, MonitorStatus status) implements ServiceMonitorStatusView {
        public UUID getServiceId() { return serviceId; }
        public MonitorStatus getStatus() { return status; }
    }

    private MonitoredService serviceWithId(UUID id, String name) {
        MonitoredService svc = org.mockito.Mockito.mock(MonitoredService.class);
        when(svc.getServiceId()).thenReturn(id);
        when(svc.getName()).thenReturn(name);
        return svc;
    }

    @Test
    void statusPageDerivesPerServiceAndOverall() {
        UUID degradedId = UUID.randomUUID();
        UUID healthyId = UUID.randomUUID();
        UUID emptyId = UUID.randomUUID();

        // Build the entity mocks into locals FIRST: doing it inline as the thenReturn argument would
        // nest when(svc...) inside when(repo.findAll()) and corrupt Mockito's stubbing (UnfinishedStubbing).
        MonitoredService payments = serviceWithId(degradedId, "Payments");
        MonitoredService auth = serviceWithId(healthyId, "Auth");
        MonitoredService reports = serviceWithId(emptyId, "Reports");
        when(monitoredServiceRepo.findAll()).thenReturn(List.of(payments, auth, reports));

        // Payments has one degraded + one operational monitor; Auth has one operational; Reports has none.
        when(httpMonitorRepo.findServiceMonitorStatuses()).thenReturn(List.of(
                new Row(degradedId, MonitorStatus.DEGRADED),
                new Row(degradedId, MonitorStatus.OPERATIONAL),
                new Row(healthyId, MonitorStatus.OPERATIONAL)));

        PublicStatusResponse response = service.getStatusPage();

        Map<UUID, ServiceStatus> byId = response.services().stream()
                .collect(Collectors.toMap(PublicServiceStatus::serviceId, PublicServiceStatus::status));

        assertEquals(ServiceStatus.DEGRADED, byId.get(degradedId));
        assertEquals(ServiceStatus.OPERATIONAL, byId.get(healthyId));
        assertEquals(ServiceStatus.UNCHECKED, byId.get(emptyId));      // no monitors -> UNCHECKED
        assertEquals(ServiceStatus.DEGRADED, response.overallStatus()); // worst wins
    }

    @Test
    void getIncidentThrows404WhenMissing() {
        UUID incidentId = UUID.randomUUID();
        when(incidentRepo.findById(incidentId)).thenReturn(Optional.empty());

        assertThrows(IncidentNotFoundException.class, () -> service.getIncident(incidentId));
    }
}
