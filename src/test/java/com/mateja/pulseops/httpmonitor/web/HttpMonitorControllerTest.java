package com.mateja.pulseops.httpmonitor.web;

import com.mateja.pulseops.common.web.GlobalExceptionHandler;
import com.mateja.pulseops.httpmonitor.application.HttpMonitorNotFoundException;
import com.mateja.pulseops.httpmonitor.application.HttpMonitorService;
import com.mateja.pulseops.httpmonitor.domain.HttpMethod;
import com.mateja.pulseops.monitoredservice.application.MonitoredServiceNotFoundException;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Web-slice test for HttpMonitorController WITH the real security filter chain (same setup as
// MonitoredServiceControllerTest). Monitors live under /api/services/{serviceId}/monitors; the
// point here is to prove ADMIN-only create/delete, authenticated reads, validation, and the 404s.
@WebMvcTest(HttpMonitorController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class HttpMonitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HttpMonitorService service;

    // SecurityConfig wires an OAuth2 resource server whose filter chain needs a JwtDecoder bean to
    // build. We authenticate via the jwt() post-processor (never calls the decoder), so a bare mock
    // is enough to satisfy the context.
    @MockitoBean
    private JwtDecoder jwtDecoder;

    // Explicit authorities — the jwt() post-processor bypasses our converter, so the "roles" claim
    // alone would not become a ROLE_* authority. These are what hasRole("ADMIN") actually checks.
    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");
    private static final SimpleGrantedAuthority RESPONDER = new SimpleGrantedAuthority("ROLE_RESPONDER");

    private static final UUID SERVICE_ID = UUID.randomUUID();

    private HttpMonitorResponse sampleResponse(UUID monitorId) {
        return new HttpMonitorResponse(
                monitorId, SERVICE_ID, "https://example.com/health",
                HttpMethod.GET, 200, true, Instant.parse("2026-07-01T12:00:00Z"));
    }

    @Test
    void createReturns201ForAdmin() throws Exception {
        UUID monitorId = UUID.randomUUID();
        CreateHttpMonitorRequest request =
                new CreateHttpMonitorRequest("https://example.com/health", HttpMethod.GET, 200);

        when(service.createMonitor(eq(SERVICE_ID), any(CreateHttpMonitorRequest.class)))
                .thenReturn(sampleResponse(monitorId));

        mockMvc.perform(post("/api/services/{serviceId}/monitors", SERVICE_ID)
                        .with(jwt().authorities(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.monitorId").value(monitorId.toString()))
                .andExpect(jsonPath("$.serviceId").value(SERVICE_ID.toString()))
                .andExpect(jsonPath("$.targetUrl").value("https://example.com/health"))
                .andExpect(jsonPath("$.httpMethod").value("GET"))
                .andExpect(jsonPath("$.expectedStatus").value(200))
                .andExpect(jsonPath("$.enabled").value(true));

        verify(service).createMonitor(eq(SERVICE_ID), any(CreateHttpMonitorRequest.class));
    }

    @Test
    void createReturns403ForResponder() throws Exception {
        CreateHttpMonitorRequest request =
                new CreateHttpMonitorRequest("https://example.com/health", HttpMethod.GET, 200);

        mockMvc.perform(post("/api/services/{serviceId}/monitors", SERVICE_ID)
                        .with(jwt().authorities(RESPONDER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void createReturns401WhenUnauthenticated() throws Exception {
        CreateHttpMonitorRequest request =
                new CreateHttpMonitorRequest("https://example.com/health", HttpMethod.GET, 200);

        mockMvc.perform(post("/api/services/{serviceId}/monitors", SERVICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void createReturns400ForInvalidExpectedStatus() throws Exception {
        // Authenticated as ADMIN so we pass the gate and reach bean validation; 10 is below @Min(100).
        CreateHttpMonitorRequest request =
                new CreateHttpMonitorRequest("https://example.com/health", HttpMethod.GET, 10);

        mockMvc.perform(post("/api/services/{serviceId}/monitors", SERVICE_ID)
                        .with(jwt().authorities(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.expectedStatus").exists());

        verifyNoInteractions(service);
    }

    @Test
    void createReturns404WhenServiceMissing() throws Exception {
        CreateHttpMonitorRequest request =
                new CreateHttpMonitorRequest("https://example.com/health", HttpMethod.GET, 200);

        when(service.createMonitor(eq(SERVICE_ID), any(CreateHttpMonitorRequest.class)))
                .thenThrow(new MonitoredServiceNotFoundException("Service not found"));

        mockMvc.perform(post("/api/services/{serviceId}/monitors", SERVICE_ID)
                        .with(jwt().authorities(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Service Not Found"));

        verify(service).createMonitor(eq(SERVICE_ID), any(CreateHttpMonitorRequest.class));
    }

    @Test
    void listReturns200ForResponder() throws Exception {
        UUID monitorId = UUID.randomUUID();
        when(service.getAllMonitors(SERVICE_ID)).thenReturn(List.of(sampleResponse(monitorId)));

        mockMvc.perform(get("/api/services/{serviceId}/monitors", SERVICE_ID)
                        .with(jwt().authorities(RESPONDER)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].monitorId").value(monitorId.toString()))
                .andExpect(jsonPath("$[0].serviceId").value(SERVICE_ID.toString()));

        verify(service).getAllMonitors(SERVICE_ID);
    }

    @Test
    void deleteReturns403ForResponder() throws Exception {
        mockMvc.perform(delete("/api/services/{serviceId}/monitors/{monitorId}", SERVICE_ID, UUID.randomUUID())
                        .with(jwt().authorities(RESPONDER)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void deleteReturns204ForAdmin() throws Exception {
        UUID monitorId = UUID.randomUUID();

        mockMvc.perform(delete("/api/services/{serviceId}/monitors/{monitorId}", SERVICE_ID, monitorId)
                        .with(jwt().authorities(ADMIN)))
                .andExpect(status().isNoContent());

        verify(service).deleteMonitor(SERVICE_ID, monitorId);
    }

    @Test
    void deleteReturns404WhenMonitorMissing() throws Exception {
        UUID monitorId = UUID.randomUUID();

        // deleteMonitor returns void, so stub with doThrow(...).when(mock).method(...).
        doThrow(new HttpMonitorNotFoundException("Monitor not found"))
                .when(service).deleteMonitor(SERVICE_ID, monitorId);

        mockMvc.perform(delete("/api/services/{serviceId}/monitors/{monitorId}", SERVICE_ID, monitorId)
                        .with(jwt().authorities(ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Monitor Not Found"));

        verify(service).deleteMonitor(SERVICE_ID, monitorId);
    }
}
