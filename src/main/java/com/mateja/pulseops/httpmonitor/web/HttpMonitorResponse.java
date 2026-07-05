package com.mateja.pulseops.httpmonitor.web;

import com.mateja.pulseops.httpmonitor.domain.HttpMethod;
import com.mateja.pulseops.httpmonitor.domain.HttpMonitor;

import java.time.Instant;
import java.util.UUID;

public record HttpMonitorResponse(UUID monitorId, UUID serviceId,
                                  String targetUrl, HttpMethod httpMethod, int expectedStatus, boolean enabled, Instant createdAt) {

    public static HttpMonitorResponse fromEntity(HttpMonitor entity) {
         if(entity == null) return null;

         return new HttpMonitorResponse(entity.getMonitorId(), entity.getMonitoredService().getServiceId()
                 ,entity.getTargetUrl(),entity.getHttpMethod(),entity.getExpectedStatus(), entity.isEnabled(), entity.getCreatedAt());
    }
}
