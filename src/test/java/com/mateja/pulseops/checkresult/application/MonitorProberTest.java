package com.mateja.pulseops.checkresult.application;

import com.mateja.pulseops.checkresult.domain.CheckResult;
import com.mateja.pulseops.checkresult.persistence.CheckResultRepo;
import com.mateja.pulseops.httpmonitor.domain.HttpMethod;
import com.mateja.pulseops.httpmonitor.domain.HttpMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class MonitorProberTest {

    private static final String URL = "http://monitor.test/health";

    private CheckResultRepo checkResultRepo;
    private MockRestServiceServer server;
    private MonitorProber prober;

    @BeforeEach
    void setUp() {
        // Bind a fake server to a real RestClient built from the same builder.
        // No sockets are opened; the server intercepts requests and returns
        // the responses we script below. This exercises the ACTUAL RestClient
        // path (onStatus swallow, status extraction) instead of mocking it.
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        checkResultRepo = mock(CheckResultRepo.class);
        prober = new MonitorProber(restClient, checkResultRepo);
    }

    @Test
    void probeRecordsSuccessWhenStatusMatchesExpected() {
        HttpMonitor monitor = monitor(URL, HttpMethod.GET, 200);
        server.expect(requestTo(URL))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));

        CheckResult result = prober.probe(monitor);

        assertTrue(result.isSuccess());
        assertEquals(200, result.getResponseStatus().intValue());
        assertNull(result.getErrorMessage());
        assertNotNull(result.getCheckedAt());
        verify(checkResultRepo).save(result);
        server.verify();
    }

    @Test
    void probeRecordsFailureWithMessageWhenStatusDiffersFromExpected() {
        HttpMonitor monitor = monitor(URL, HttpMethod.GET, 200);
        server.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        CheckResult result = prober.probe(monitor);

        assertFalse(result.isSuccess());
        assertEquals(500, result.getResponseStatus().intValue());
        assertEquals("expected 200 got 500", result.getErrorMessage());
        verify(checkResultRepo).save(result);
        server.verify();
    }

    @Test
    void probeRecordsFailureWithNullStatusWhenRequestFails() {
        HttpMonitor monitor = monitor(URL, HttpMethod.GET, 200);
        // A transport-level failure (no HTTP response at all): DNS, refused,
        // timeout. RestClient wraps it; our probe's generic catch handles it.
        server.expect(requestTo(URL))
                .andRespond(withException(new IOException("Connection refused")));

        CheckResult result = prober.probe(monitor);

        assertFalse(result.isSuccess());
        assertNull(result.getResponseStatus());
        assertNotNull(result.getErrorMessage());
        verify(checkResultRepo).save(result);
    }

    private HttpMonitor monitor(String url, HttpMethod method, int expectedStatus) {
        HttpMonitor monitor = mock(HttpMonitor.class);
        when(monitor.getTargetUrl()).thenReturn(url);
        when(monitor.getHttpMethod()).thenReturn(method);
        when(monitor.getExpectedStatus()).thenReturn(expectedStatus);
        return monitor;
    }
}
