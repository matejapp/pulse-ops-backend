package com.mateja.pulseops.incident.web;

import com.mateja.pulseops.common.web.GlobalExceptionHandler;
import com.mateja.pulseops.incident.application.IncidentNotFoundException;
import com.mateja.pulseops.incident.application.IncidentService;
import com.mateja.pulseops.incident.domain.IncidentStatus;
import com.mateja.pulseops.incident.domain.InvalidIncidentTransitionException;
import com.mateja.pulseops.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Web-slice test for IncidentController with the real security filter chain. Incidents are
// read+write for ANY authenticated user (no ADMIN gate), so there is NO 403 case to assert —
// only authenticated-vs-401. We use RESPONDER throughout to prove a non-admin can also write.
@WebMvcTest(IncidentController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IncidentService service;

    // Present only so the OAuth2 resource-server filter chain can build; jwt() bypasses it.
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final SimpleGrantedAuthority RESPONDER = new SimpleGrantedAuthority("ROLE_RESPONDER");

    private static final UUID SERVICE_ID = UUID.randomUUID();
    private static final UUID INCIDENT_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.parse("2026-07-06T10:00:00Z");

    private IncidentResponse sampleIncident(IncidentStatus status, Instant resolvedAt) {
        return new IncidentResponse(INCIDENT_ID, SERVICE_ID, "Payments API down", status, CREATED_AT, resolvedAt);
    }

    private IncidentDetailResponse sampleDetail(IncidentStatus status) {
        IncidentUpdateResponse opening = new IncidentUpdateResponse(
                UUID.randomUUID(), INCIDENT_ID, IncidentStatus.INVESTIGATING, "Looking into it", CREATED_AT);
        IncidentUpdateResponse latest = new IncidentUpdateResponse(
                UUID.randomUUID(), INCIDENT_ID, status, "Now " + status, CREATED_AT.plusSeconds(60));
        return new IncidentDetailResponse(sampleIncident(status, null), List.of(opening, latest));
    }

    @Test
    void openReturns201ForResponder() throws Exception {
        OpenIncidentRequest request = new OpenIncidentRequest(SERVICE_ID, "Payments API down", "Looking into it");
        when(service.openIncident(any(OpenIncidentRequest.class)))
                .thenReturn(sampleIncident(IncidentStatus.INVESTIGATING, null));

        mockMvc.perform(post("/api/incidents")
                        .with(jwt().authorities(RESPONDER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.incidentId").value(INCIDENT_ID.toString()))
                .andExpect(jsonPath("$.serviceId").value(SERVICE_ID.toString()))
                .andExpect(jsonPath("$.title").value("Payments API down"))
                .andExpect(jsonPath("$.currentStatus").value("INVESTIGATING"))
                .andExpect(jsonPath("$.resolvedAt").doesNotExist());

        verify(service).openIncident(any(OpenIncidentRequest.class));
    }

    @Test
    void openReturns401WhenUnauthenticated() throws Exception {
        OpenIncidentRequest request = new OpenIncidentRequest(SERVICE_ID, "Payments API down", "Looking into it");

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void openReturns400ForBlankTitle() throws Exception {
        // Authenticated so we pass the gate and reach bean validation; blank title fails @NotBlank.
        OpenIncidentRequest request = new OpenIncidentRequest(SERVICE_ID, "", "Looking into it");

        mockMvc.perform(post("/api/incidents")
                        .with(jwt().authorities(RESPONDER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors.title").exists());

        verifyNoInteractions(service);
    }

    @Test
    void addUpdateReturns200WithRefreshedTimeline() throws Exception {
        AddIncidentUpdateRequest request = new AddIncidentUpdateRequest(IncidentStatus.MONITORING, "Now MONITORING");
        when(service.addUpdate(eq(INCIDENT_ID), any(AddIncidentUpdateRequest.class)))
                .thenReturn(sampleDetail(IncidentStatus.MONITORING));

        mockMvc.perform(post("/api/incidents/{incidentId}/updates", INCIDENT_ID)
                        .with(jwt().authorities(RESPONDER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.incident.incidentId").value(INCIDENT_ID.toString()))
                .andExpect(jsonPath("$.incident.currentStatus").value("MONITORING"))
                .andExpect(jsonPath("$.updates.length()").value(2))
                .andExpect(jsonPath("$.updates[1].status").value("MONITORING"));

        verify(service).addUpdate(eq(INCIDENT_ID), any(AddIncidentUpdateRequest.class));
    }

    @Test
    void addUpdateReturns404WhenIncidentMissing() throws Exception {
        AddIncidentUpdateRequest request = new AddIncidentUpdateRequest(IncidentStatus.MONITORING, "Now MONITORING");
        when(service.addUpdate(eq(INCIDENT_ID), any(AddIncidentUpdateRequest.class)))
                .thenThrow(new IncidentNotFoundException("Incident not found"));

        mockMvc.perform(post("/api/incidents/{incidentId}/updates", INCIDENT_ID)
                        .with(jwt().authorities(RESPONDER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Incident Not Found"));

        verify(service).addUpdate(eq(INCIDENT_ID), any(AddIncidentUpdateRequest.class));
    }

    @Test
    void addUpdateReturns409OnIllegalTransition() throws Exception {
        AddIncidentUpdateRequest request = new AddIncidentUpdateRequest(IncidentStatus.INVESTIGATING, "backwards");
        when(service.addUpdate(eq(INCIDENT_ID), any(AddIncidentUpdateRequest.class)))
                .thenThrow(new InvalidIncidentTransitionException("MONITORING -> INVESTIGATING"));

        mockMvc.perform(post("/api/incidents/{incidentId}/updates", INCIDENT_ID)
                        .with(jwt().authorities(RESPONDER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid Incident Transition"));

        verify(service).addUpdate(eq(INCIDENT_ID), any(AddIncidentUpdateRequest.class));
    }

    @Test
    void listReturns200ForResponder() throws Exception {
        when(service.getAll()).thenReturn(List.of(sampleIncident(IncidentStatus.INVESTIGATING, null)));

        mockMvc.perform(get("/api/incidents")
                        .with(jwt().authorities(RESPONDER)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].incidentId").value(INCIDENT_ID.toString()))
                .andExpect(jsonPath("$[0].currentStatus").value("INVESTIGATING"));

        verify(service).getAll();
    }

    @Test
    void getOneReturns200WithTimeline() throws Exception {
        when(service.getOne(INCIDENT_ID)).thenReturn(sampleDetail(IncidentStatus.MONITORING));

        mockMvc.perform(get("/api/incidents/{incidentId}", INCIDENT_ID)
                        .with(jwt().authorities(RESPONDER)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.incident.incidentId").value(INCIDENT_ID.toString()))
                .andExpect(jsonPath("$.updates.length()").value(2));

        verify(service).getOne(INCIDENT_ID);
    }

    @Test
    void getOneReturns404WhenIncidentMissing() throws Exception {
        when(service.getOne(INCIDENT_ID)).thenThrow(new IncidentNotFoundException("Incident not found"));

        mockMvc.perform(get("/api/incidents/{incidentId}", INCIDENT_ID)
                        .with(jwt().authorities(RESPONDER)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Incident Not Found"));

        verify(service).getOne(INCIDENT_ID);
    }
}
