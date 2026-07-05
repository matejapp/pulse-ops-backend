package com.mateja.pulseops.checkresult.application;

import com.mateja.pulseops.httpmonitor.domain.HttpMonitor;
import com.mateja.pulseops.httpmonitor.persistence.HttpMonitorRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckSchedulerTest {

    @Mock
    private HttpMonitorRepo httpMonitorRepo;

    @Mock
    private MonitorProber monitorProber;

    private ExecutorService executorService;
    private CheckScheduler scheduler;

    @BeforeEach
    void setUp() {
        // A real pool so the submit/join path runs for real; runCheck() blocks
        // on the futures, so by the time it returns every probe has completed.
        executorService = Executors.newFixedThreadPool(2);
        scheduler = new CheckScheduler(httpMonitorRepo, monitorProber, executorService);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void runCheckProbesEveryEnabledMonitor() {
        HttpMonitor first = mock();
        HttpMonitor second = mock();
        when(httpMonitorRepo.findByEnabledTrue()).thenReturn(List.of(first, second));

        scheduler.runCheck();

        verify(monitorProber).probe(first);
        verify(monitorProber).probe(second);
    }

    @Test
    void runCheckDoesNothingWhenNoEnabledMonitors() {
        when(httpMonitorRepo.findByEnabledTrue()).thenReturn(List.of());

        scheduler.runCheck();

        verify(monitorProber, never()).probe(any());
    }

    private static HttpMonitor mock() {
        return org.mockito.Mockito.mock(HttpMonitor.class);
    }
}
