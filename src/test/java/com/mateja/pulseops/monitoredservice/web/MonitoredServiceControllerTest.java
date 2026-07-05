package com.mateja.pulseops.monitoredservice.web;

import com.mateja.pulseops.common.web.GlobalExceptionHandler;
import com.mateja.pulseops.monitoredservice.application.MonitoredServiceService;
import com.mateja.pulseops.monitoredservice.application.ServiceAlreadyExistsException;
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

// Web-slice test for MonitoredServiceController WITH the real security filter chain enabled.
// AuthControllerTest disables filters (addFilters=false) because it only tests mapping/validation;
// here the whole point is to prove the ADMIN-only authorization, so we import the real SecurityConfig
// and leave filters ON. The service layer is mocked — no DB, no business logic under test.
@WebMvcTest(MonitoredServiceController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class MonitoredServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MonitoredServiceService service;

    // SecurityConfig turns the app into an OAuth2 resource server, whose filter chain needs a
    // JwtDecoder bean to even build. We authenticate via the jwt() request post-processor (which
    // injects the Authentication directly and never calls the decoder), so a bare mock suffices.
    @MockitoBean
    private JwtDecoder jwtDecoder;

    // The jwt() post-processor bypasses our JwtAuthenticationConverter, so a "roles" claim alone
    // would NOT become a ROLE_* authority. We set the authority explicitly — that is what the
    // hasRole("ADMIN") matcher in SecurityConfig actually checks against.
    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");
    private static final SimpleGrantedAuthority RESPONDER = new SimpleGrantedAuthority("ROLE_RESPONDER");

    @Test
    void createReturns201ForAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-07-01T12:00:00Z");

        CreateServiceRequest request = new CreateServiceRequest("Payments API", "handles payments");
        ServiceResponse response = new ServiceResponse(id, "Payments API", "handles payments", createdAt);

        when(service.createMonitoredService(any(CreateServiceRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/services")
                        .with(jwt().authorities(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.serviceId").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Payments API"))
                .andExpect(jsonPath("$.description").value("handles payments"))
                .andExpect(jsonPath("$.createdAt").value("2026-07-01T12:00:00Z"));

        verify(service).createMonitoredService(any(CreateServiceRequest.class));
    }

    @Test
    void createReturns403ForResponder() throws Exception {
        CreateServiceRequest request = new CreateServiceRequest("Payments API", "handles payments");

        mockMvc.perform(post("/api/services")
                        .with(jwt().authorities(RESPONDER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isForbidden());

        // Blocked at the security layer — the controller/service must never be reached.
        verifyNoInteractions(service);
    }

    @Test
    void createReturns401WhenUnauthenticated() throws Exception {
        CreateServiceRequest request = new CreateServiceRequest("Payments API", "handles payments");

        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void createReturns400ForBlankName() throws Exception {
        // Authenticated as ADMIN so we pass the authorization gate and reach bean validation;
        // a blank name violates @NotBlank on CreateServiceRequest.
        CreateServiceRequest request = new CreateServiceRequest("", "handles payments");

        mockMvc.perform(post("/api/services")
                        .with(jwt().authorities(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());

        verifyNoInteractions(service);
    }

    @Test
    void listReturns200ForResponder() throws Exception {
        UUID id = UUID.randomUUID();
        ServiceResponse response = new ServiceResponse(
                id, "Payments API", "handles payments", Instant.parse("2026-07-01T12:00:00Z"));

        when(service.getAllServices()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/services").with(jwt().authorities(RESPONDER)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].serviceId").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("Payments API"));

        verify(service).getAllServices();
    }

    @Test
    void deleteReturns403ForResponder() throws Exception {
        mockMvc.perform(delete("/api/services/{id}", UUID.randomUUID())
                        .with(jwt().authorities(RESPONDER)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void deleteReturns204ForAdmin() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/services/{id}", id).with(jwt().authorities(ADMIN)))
                .andExpect(status().isNoContent());

        verify(service).deleteMonitoredService(id);
    }

    @Test
    void createReturns409ForDuplicateName() throws Exception {
        CreateServiceRequest request = new CreateServiceRequest("Payments API", "handles payments");

        when(service.createMonitoredService(any(CreateServiceRequest.class)))
                .thenThrow(new ServiceAlreadyExistsException("Service Already Exists"));

        mockMvc.perform(post("/api/services")
                        .with(jwt().authorities(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Service Already Exists"))
                .andExpect(jsonPath("$.detail").value("Service Already Exists"));

        verify(service).createMonitoredService(any(CreateServiceRequest.class));
    }
}
