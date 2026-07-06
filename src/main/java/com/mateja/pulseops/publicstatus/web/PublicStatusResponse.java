package com.mateja.pulseops.publicstatus.web;

import com.mateja.pulseops.publicstatus.domain.ServiceStatus;

import java.util.List;

// The public status page payload: an overall banner status plus one row per service.
public record PublicStatusResponse(ServiceStatus overallStatus, List<PublicServiceStatus> services) {
}
