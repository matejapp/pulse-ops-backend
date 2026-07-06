package com.mateja.pulseops.publicstatus.web;

import com.mateja.pulseops.common.web.GlobalExceptionHandler;
import com.mateja.pulseops.incident.application.IncidentNotFoundException;
import com.mateja.pulseops.incident.domain.IncidentStatus;
import com.mateja.pulseops.incident.web.IncidentUpdateResponse;
import com.mateja.pulseops.publicstatus.application.PublicStatusService;
import com.mateja.pulseops.publicstatus.domain.ServiceStatus;
import com.mateja.pulseops.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Web-slice test for the PUBLIC status API with the real security filter chain. The whole point is
// that /api/public/** is permitAll: every request here runs with NO token and must still return 200
// (or 404 for a missing incident) — never 401.
@WebMvcTest(PublicStatusController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PublicStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicStatusService service;

    // Present only so the OAuth2 resource-server filter chain can build; public routes never call it.
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final UUID SERVICE_ID = UUID.randomUUID();
    private static final UUID INCIDENT_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.parse("2026-07-06T10:00:00Z");

    @Test
    void statusPageReturns200WithoutAuthentication() throws Exception {
        PublicStatusResponse response = new PublicStatusResponse(
                ServiceStatus.DEGRADED,
                List.of(new PublicServiceStatus(SERVICE_ID, "Payments", ServiceStatus.DEGRADED)));
        when(service.getStatusPage()).thenReturn(response);

        // No .with(jwt(...)) — proves the endpoint is public.
        mockMvc.perform(get("/api/public/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.overallStatus").value("DEGRADED"))
                .andExpect(jsonPath("$.services[0].serviceId").value(SERVICE_ID.toString()))
                .andExpect(jsonPath("$.services[0].name").value("Payments"))
                .andExpect(jsonPath("$.services[0].status").value("DEGRADED"));

        verify(service).getStatusPage();
    }

    @Test
    void activeIncidentsReturns200WithoutAuthentication() throws Exception {
        PublicIncidentResponse incident = new PublicIncidentResponse(
                INCIDENT_ID, "Payments", "Payments API down", IncidentStatus.INVESTIGATING, CREATED_AT, null);
        when(service.getActiveIncidents()).thenReturn(List.of(incident));

        mockMvc.perform(get("/api/public/incidents"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].incidentId").value(INCIDENT_ID.toString()))
                .andExpect(jsonPath("$[0].serviceName").value("Payments"))
                .andExpect(jsonPath("$[0].currentStatus").value("INVESTIGATING"));

        verify(service).getActiveIncidents();
    }

    @Test
    void incidentDetailReturns200WithoutAuthentication() throws Exception {
        PublicIncidentResponse incident = new PublicIncidentResponse(
                INCIDENT_ID, "Payments", "Payments API down", IncidentStatus.MONITORING, CREATED_AT, null);
        IncidentUpdateResponse update = new IncidentUpdateResponse(
                UUID.randomUUID(), INCIDENT_ID, IncidentStatus.INVESTIGATING, "Looking into it", CREATED_AT);
        when(service.getIncident(INCIDENT_ID))
                .thenReturn(new PublicIncidentDetailResponse(incident, List.of(update)));

        mockMvc.perform(get("/api/public/incidents/{incidentId}", INCIDENT_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.incident.incidentId").value(INCIDENT_ID.toString()))
                .andExpect(jsonPath("$.incident.serviceName").value("Payments"))
                .andExpect(jsonPath("$.updates.length()").value(1))
                .andExpect(jsonPath("$.updates[0].message").value("Looking into it"));

        verify(service).getIncident(INCIDENT_ID);
    }

    @Test
    void incidentDetailReturns404WhenMissing() throws Exception {
        when(service.getIncident(INCIDENT_ID)).thenThrow(new IncidentNotFoundException("Incident not found"));

        mockMvc.perform(get("/api/public/incidents/{incidentId}", INCIDENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Incident Not Found"));

        verify(service).getIncident(INCIDENT_ID);
    }
}
