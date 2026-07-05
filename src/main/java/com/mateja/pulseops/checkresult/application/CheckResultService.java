package com.mateja.pulseops.checkresult.application;

import com.mateja.pulseops.checkresult.persistence.CheckResultRepo;
import com.mateja.pulseops.checkresult.web.CheckResultResponse;
import com.mateja.pulseops.httpmonitor.application.HttpMonitorNotFoundException;
import com.mateja.pulseops.httpmonitor.persistence.HttpMonitorRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CheckResultService {

    private final HttpMonitorRepo  httpMonitorRepo;
    private final CheckResultRepo  checkResultRepo;

    public  CheckResultService(HttpMonitorRepo httpMonitorRepo,  CheckResultRepo  checkResultRepo) {
        this.httpMonitorRepo = httpMonitorRepo;
        this.checkResultRepo = checkResultRepo;
    }

    @Transactional(readOnly = true)
    public PagedModel<CheckResultResponse> getHistory(UUID monitorId, Pageable pageable){
        if(!httpMonitorRepo.existsById(monitorId)){
            throw new HttpMonitorNotFoundException("Monitor not found");
        }
        Page<CheckResultResponse> page =  checkResultRepo.findByMonitor_MonitorId(monitorId,pageable).map(CheckResultResponse::fromEntity);

        return new PagedModel<>(page);
    }

}
