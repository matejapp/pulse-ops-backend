package com.mateja.pulseops.monitoredservice.web;

import com.mateja.pulseops.monitoredservice.domain.MonitoredService;

import java.time.Instant;
import java.util.UUID;

public record ServiceResponse (UUID serviceId, String name, String description, Instant createdAt) {
    public static ServiceResponse fromEntity(MonitoredService monitoredService) {
        if(monitoredService == null) return null;

        return new ServiceResponse(monitoredService.getServiceId(), monitoredService.getName(), monitoredService.getDescription(), monitoredService.getCreatedAt());
    }
}
