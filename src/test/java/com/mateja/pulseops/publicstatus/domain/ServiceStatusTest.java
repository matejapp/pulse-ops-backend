package com.mateja.pulseops.publicstatus.domain;

import com.mateja.pulseops.httpmonitor.domain.MonitorStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Pure aggregation logic: precedence DEGRADED > OPERATIONAL > UNCHECKED, for both the per-service
// roll-up (fromMonitors) and the overall roll-up (worstOf). No Spring, no mocks.
class ServiceStatusTest {

    // --- fromMonitors: monitor statuses -> service status ---

    @Test
    void noMonitorsIsUnchecked() {
        assertEquals(ServiceStatus.UNCHECKED, ServiceStatus.fromMonitors(List.of()));
    }

    @Test
    void allOperationalIsOperational() {
        assertEquals(ServiceStatus.OPERATIONAL,
                ServiceStatus.fromMonitors(List.of(MonitorStatus.OPERATIONAL, MonitorStatus.OPERATIONAL)));
    }

    @Test
    void anyDegradedMakesServiceDegraded() {
        assertEquals(ServiceStatus.DEGRADED,
                ServiceStatus.fromMonitors(List.of(MonitorStatus.OPERATIONAL, MonitorStatus.DEGRADED)));
    }

    @Test
    void operationalWithUncheckedIsOperational() {
        assertEquals(ServiceStatus.OPERATIONAL,
                ServiceStatus.fromMonitors(List.of(MonitorStatus.UNCHECKED, MonitorStatus.OPERATIONAL)));
    }

    @Test
    void allUncheckedIsUnchecked() {
        assertEquals(ServiceStatus.UNCHECKED,
                ServiceStatus.fromMonitors(List.of(MonitorStatus.UNCHECKED, MonitorStatus.UNCHECKED)));
    }

    // --- worstOf: service statuses -> overall ---

    @Test
    void worstOfEmptyIsUnchecked() {
        assertEquals(ServiceStatus.UNCHECKED, ServiceStatus.worstOf(List.of()));
    }

    @Test
    void worstOfWithDegradedIsDegraded() {
        assertEquals(ServiceStatus.DEGRADED,
                ServiceStatus.worstOf(List.of(ServiceStatus.OPERATIONAL, ServiceStatus.DEGRADED, ServiceStatus.UNCHECKED)));
    }

    @Test
    void worstOfOperationalAndUncheckedIsOperational() {
        assertEquals(ServiceStatus.OPERATIONAL,
                ServiceStatus.worstOf(List.of(ServiceStatus.OPERATIONAL, ServiceStatus.UNCHECKED)));
    }
}
