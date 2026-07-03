package com.mateja.pulseops.monitoredservice.application;

import com.mateja.pulseops.monitoredservice.web.CreateServiceRequest;
import com.mateja.pulseops.monitoredservice.web.ServiceResponse;
import com.mateja.pulseops.monitoredservice.domain.MonitoredService;
import com.mateja.pulseops.monitoredservice.persistence.MonitoredServiceRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MonitoredServiceService {

    private final MonitoredServiceRepo repo;

    public MonitoredServiceService(MonitoredServiceRepo repo) {
        this.repo = repo;
    }

    @Transactional
    public ServiceResponse createMonitoredService(CreateServiceRequest createSr)
    {
        String name = createSr.name();

        if(repo.existsByNameIgnoreCase(name)){
            throw new ServiceAlreadyExistsException("Service Already Exists");
        }

        MonitoredService entity = new MonitoredService(createSr.name(), createSr.description());
        repo.saveAndFlush(entity);

        return ServiceResponse.fromEntity(entity);
    }

    @Transactional
    public void deleteMonitoredService(UUID id)
    {
        if(!repo.existsById(id)){
            throw new MonitoredServiceNotFoundException("Service Not Found!");
        }

        repo.deleteById(id);
    }

    public List<ServiceResponse> getAllServices() {
        List<MonitoredService> services = repo.findAll();

        return services.stream().
                map(ServiceResponse::fromEntity)
                .toList();
    }
}
