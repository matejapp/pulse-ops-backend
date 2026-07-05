package com.mateja.pulseops.checkresult.web;

import com.mateja.pulseops.checkresult.application.CheckResultService;
import com.mateja.pulseops.common.web.GlobalExceptionHandler;
import com.mateja.pulseops.httpmonitor.application.HttpMonitorNotFoundException;
import com.mateja.pulseops.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Web-slice test for CheckResultController with the real security filter chain. The history
// endpoint is a plain authenticated GET (no ADMIN gate); we prove 200 + paged JSON, 401 when
// unauthenticated, and 404 when the monitor is unknown.
@WebMvcTest(CheckResultController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CheckResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckResultService service;

    // Present only so the OAuth2 resource-server filter chain can build; jwt() bypasses it.
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final SimpleGrantedAuthority RESPONDER = new SimpleGrantedAuthority("ROLE_RESPONDER");
    private static final UUID MONITOR_ID = UUID.randomUUID();

    private PagedModel<CheckResultResponse> onePage(UUID checkResultId) {
        CheckResultResponse response = new CheckResultResponse(
                checkResultId, MONITOR_ID, false, 500, 145,
                "expected 200 got 500", Instant.parse("2026-07-05T19:47:48Z"));
        Page<CheckResultResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);
        return new PagedModel<>(page);
    }

    @Test
    void historyReturns200WithPagedResultsForAuthenticatedUser() throws Exception {
        UUID checkResultId = UUID.randomUUID();
        when(service.getHistory(eq(MONITOR_ID), any(Pageable.class))).thenReturn(onePage(checkResultId));

        mockMvc.perform(get("/api/monitors/{monitorId}/results", MONITOR_ID)
                        .with(jwt().authorities(RESPONDER)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].checkResultId").value(checkResultId.toString()))
                .andExpect(jsonPath("$.content[0].monitorId").value(MONITOR_ID.toString()))
                .andExpect(jsonPath("$.content[0].success").value(false))
                .andExpect(jsonPath("$.content[0].responseStatus").value(500))
                .andExpect(jsonPath("$.content[0].errorMessage").value("expected 200 got 500"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        verify(service).getHistory(eq(MONITOR_ID), any(Pageable.class));
    }

    @Test
    void historyReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/monitors/{monitorId}/results", MONITOR_ID))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void historyReturns404WhenMonitorMissing() throws Exception {
        when(service.getHistory(eq(MONITOR_ID), any(Pageable.class)))
                .thenThrow(new HttpMonitorNotFoundException("Monitor not found"));

        mockMvc.perform(get("/api/monitors/{monitorId}/results", MONITOR_ID)
                        .with(jwt().authorities(RESPONDER)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Monitor Not Found"));

        verify(service).getHistory(eq(MONITOR_ID), any(Pageable.class));
    }
}
