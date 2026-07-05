package com.mateja.pulseops.checkresult.application;

import com.mateja.pulseops.checkresult.domain.CheckResult;
import com.mateja.pulseops.checkresult.persistence.CheckResultRepo;
import com.mateja.pulseops.httpmonitor.domain.HttpMonitor;
import com.mateja.pulseops.httpmonitor.persistence.HttpMonitorRepo;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CheckResultWriter {

    private final HttpMonitorRepo httpMonitorRepo;
    private final CheckResultRepo checkResultRepo;

    public CheckResultWriter(HttpMonitorRepo httpMonitorRepo, CheckResultRepo checkResultRepo) {
        this.httpMonitorRepo = httpMonitorRepo;
        this.checkResultRepo = checkResultRepo;
    }

    @Transactional
    public void record(HttpMonitor monitor, CheckResult result) {
        httpMonitorRepo.save(monitor);
        checkResultRepo.save(result);
    }
}
