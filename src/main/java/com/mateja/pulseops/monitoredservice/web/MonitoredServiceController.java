package com.mateja.pulseops.monitoredservice.web;

import com.mateja.pulseops.monitoredservice.application.MonitoredServiceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/services")
public class MonitoredServiceController {

    private final MonitoredServiceService service;

    public MonitoredServiceController(MonitoredServiceService service) {
        this.service = service;
    }


    @PostMapping()
    public ResponseEntity<ServiceResponse> createService(@RequestBody @Valid CreateServiceRequest createServiceRequest) {
        ServiceResponse createdService = service.createMonitoredService(createServiceRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdService);
    }

    @GetMapping()
    public ResponseEntity<List<ServiceResponse>> getServices() {
        List<ServiceResponse> services = service.getAllServices();
        return ResponseEntity.status(HttpStatus.OK).body(services);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable UUID id) {
        service.deleteMonitoredService(id);
        return ResponseEntity.noContent().build();
    }
}
