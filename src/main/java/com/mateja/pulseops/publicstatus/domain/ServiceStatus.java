package com.mateja.pulseops.publicstatus.domain;

import com.mateja.pulseops.httpmonitor.domain.MonitorStatus;

import java.util.Collection;

// The DERIVED status of a service (and of the whole system), computed at read time from its
// monitors' statuses — there is NO stored service-status column (M5 decision: authoritative status
// lives on the monitor; the service view is aggregated on demand to avoid per-probe writes + drift).
// Precedence, worst first: DEGRADED > OPERATIONAL > UNCHECKED.
public enum ServiceStatus {
    OPERATIONAL,
    DEGRADED,
    UNCHECKED;

    // Roll a service's monitor statuses up to one service status. Any DEGRADED monitor degrades the
    // service; otherwise any OPERATIONAL monitor makes it operational; otherwise (no monitors, or all
    // UNCHECKED) the service is UNCHECKED.
    public static ServiceStatus fromMonitors(Collection<MonitorStatus> monitorStatuses) {
        if (monitorStatuses.contains(MonitorStatus.DEGRADED)) return DEGRADED;
        if (monitorStatuses.contains(MonitorStatus.OPERATIONAL)) return OPERATIONAL;
        return UNCHECKED;
    }

    // Roll per-service statuses up to the overall system status (same precedence). Empty -> UNCHECKED.
    public static ServiceStatus worstOf(Collection<ServiceStatus> serviceStatuses) {
        if (serviceStatuses.contains(DEGRADED)) return DEGRADED;
        if (serviceStatuses.contains(OPERATIONAL)) return OPERATIONAL;
        return UNCHECKED;
    }
}
