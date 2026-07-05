package com.mateja.pulseops.httpmonitor.application;

import com.mateja.pulseops.httpmonitor.domain.HttpMonitor;
import com.mateja.pulseops.httpmonitor.persistence.HttpMonitorRepo;
import com.mateja.pulseops.httpmonitor.web.CreateHttpMonitorRequest;
import com.mateja.pulseops.httpmonitor.web.HttpMonitorResponse;
import com.mateja.pulseops.monitoredservice.application.MonitoredServiceNotFoundException;
import com.mateja.pulseops.monitoredservice.domain.MonitoredService;
import com.mateja.pulseops.monitoredservice.persistence.MonitoredServiceRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class HttpMonitorService {

    private final HttpMonitorRepo httpMonitorRepo;
    private final MonitoredServiceRepo monitoredServiceRepo;

    public HttpMonitorService(HttpMonitorRepo httpMonitorRepo, MonitoredServiceRepo monitoredServiceRepo) {
        this.httpMonitorRepo = httpMonitorRepo;
        this.monitoredServiceRepo = monitoredServiceRepo;
    }

    @Transactional
    public List<HttpMonitorResponse> getAllMonitors(UUID serviceId) {
        Optional<MonitoredService> monitoredServiceFound = monitoredServiceRepo.findById(serviceId);

        if(monitoredServiceFound.isEmpty()) throw new MonitoredServiceNotFoundException("Service not found");

        MonitoredService monitoredService = monitoredServiceFound.get();

        List<HttpMonitor> monitors = httpMonitorRepo.findByMonitoredService_ServiceId(monitoredService.getServiceId());

        return monitors.stream().map(HttpMonitorResponse::fromEntity).toList();
    }

    @Transactional
    public HttpMonitorResponse createMonitor(UUID serviceId, CreateHttpMonitorRequest createHttpMonitorRequest) {
        Optional<MonitoredService> monitoredServiceFound = monitoredServiceRepo.findById(serviceId);

        if(monitoredServiceFound.isEmpty()) throw new MonitoredServiceNotFoundException("Service not found");

        MonitoredService monitoredService = monitoredServiceFound.get();

        HttpMonitor entity = new HttpMonitor(monitoredService,createHttpMonitorRequest.targetUrl()
                ,createHttpMonitorRequest.httpMethod(),createHttpMonitorRequest.expectedStatus());

        httpMonitorRepo.save(entity);
        return HttpMonitorResponse.fromEntity(entity);
    }

    @Transactional
    public void deleteMonitor(UUID serviceId, UUID monitorId) {
        if (!monitoredServiceRepo.existsById(serviceId)) {
            throw new MonitoredServiceNotFoundException("Service not found");
        }

        HttpMonitor monitor = httpMonitorRepo.findById(monitorId)
                .orElseThrow(() -> new HttpMonitorNotFoundException("Monitor not found"));

        // Ownership check: the monitor's PARENT service id must equal the serviceId in the path.
        // Without this, DELETE /api/services/{anyService}/monitors/{someMonitor} would delete a
        // monitor that belongs to a different service. Reached through the monitor -> parent -> id.
        if (!monitor.getMonitoredService().getServiceId().equals(serviceId)) {
            throw new HttpMonitorNotFoundException("Monitor not found in this service");
        }

        httpMonitorRepo.delete(monitor);
    }
}
