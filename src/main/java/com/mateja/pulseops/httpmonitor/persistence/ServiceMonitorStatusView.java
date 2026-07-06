package com.mateja.pulseops.httpmonitor.persistence;

import com.mateja.pulseops.httpmonitor.domain.MonitorStatus;

import java.util.UUID;

// Spring Data interface projection: pulls only (serviceId, monitor status) pairs for every monitor
// in ONE query, so the public status page can derive per-service status without loading full
// HttpMonitor rows or walking the LAZY service association per monitor (no N+1).
public interface ServiceMonitorStatusView {
    UUID getServiceId();

    MonitorStatus getStatus();
}
