package com.mateja.pulseops.httpmonitor.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Pure domain logic: the OPERATIONAL/DEGRADED/UNCHECKED state machine driven by the
// consecutive-failure counter. No Spring, no mocks, no DB — just new + assert.
// The MonitoredService parent is irrelevant to these transitions, so we pass null.
class HttpMonitorTest {

    private HttpMonitor newMonitor() {
        return new HttpMonitor(null, "http://monitor.test/health", HttpMethod.GET, 200);
    }

    @Test
    void freshMonitorStartsUncheckedWithNoFailures() {
        HttpMonitor monitor = newMonitor();

        assertEquals(MonitorStatus.UNCHECKED, monitor.getStatus());
        assertEquals(0, monitor.getConsecutiveFailures());
    }

    @Test
    void failuresBelowThresholdStayUncheckedButCountUp() {
        HttpMonitor monitor = newMonitor();

        monitor.recordFailure();
        monitor.recordFailure();

        assertEquals(MonitorStatus.UNCHECKED, monitor.getStatus());
        assertEquals(2, monitor.getConsecutiveFailures());
    }

    @Test
    void threeConsecutiveFailuresMarkDegraded() {
        HttpMonitor monitor = newMonitor();

        monitor.recordFailure();
        monitor.recordFailure();
        monitor.recordFailure();

        assertEquals(MonitorStatus.DEGRADED, monitor.getStatus());
        assertEquals(3, monitor.getConsecutiveFailures());
    }

    @Test
    void furtherFailuresStayDegradedAndKeepCounting() {
        HttpMonitor monitor = newMonitor();

        monitor.recordFailure();
        monitor.recordFailure();
        monitor.recordFailure();
        monitor.recordFailure();

        assertEquals(MonitorStatus.DEGRADED, monitor.getStatus());
        assertEquals(4, monitor.getConsecutiveFailures());
    }

    @Test
    void oneSuccessRecoversFromDegradedAndResetsCounter() {
        HttpMonitor monitor = newMonitor();
        monitor.recordFailure();
        monitor.recordFailure();
        monitor.recordFailure(); // now DEGRADED

        monitor.recordSuccess();

        assertEquals(MonitorStatus.OPERATIONAL, monitor.getStatus());
        assertEquals(0, monitor.getConsecutiveFailures());
    }

    @Test
    void successFromFreshMonitorGoesOperational() {
        HttpMonitor monitor = newMonitor();

        monitor.recordSuccess();

        assertEquals(MonitorStatus.OPERATIONAL, monitor.getStatus());
        assertEquals(0, monitor.getConsecutiveFailures());
    }

    @Test
    void successThenFailuresRestartTheStreak() {
        HttpMonitor monitor = newMonitor();
        monitor.recordSuccess();      // OPERATIONAL, 0
        monitor.recordFailure();      // 1
        monitor.recordFailure();      // 2

        assertEquals(MonitorStatus.OPERATIONAL, monitor.getStatus());
        assertEquals(2, monitor.getConsecutiveFailures());
    }
}
