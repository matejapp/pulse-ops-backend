package com.mateja.pulseops.checkresult.application;

import com.mateja.pulseops.checkresult.domain.CheckResult;
import com.mateja.pulseops.checkresult.persistence.CheckResultRepo;
import com.mateja.pulseops.httpmonitor.domain.HttpMonitor;
import com.mateja.pulseops.httpmonitor.persistence.HttpMonitorRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CheckResultWriterTest {

    @Mock
    private HttpMonitorRepo httpMonitorRepo;

    @Mock
    private CheckResultRepo checkResultRepo;

    @InjectMocks
    private CheckResultWriter writer;

    @Test
    void recordPersistsBothTheMonitorAndTheResult() {
        HttpMonitor monitor = mock(HttpMonitor.class);
        CheckResult result = mock(CheckResult.class);

        writer.record(monitor, result);

        verify(httpMonitorRepo).save(monitor);
        verify(checkResultRepo).save(result);
    }
}
