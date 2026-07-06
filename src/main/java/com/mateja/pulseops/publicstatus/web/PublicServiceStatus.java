package com.mateja.pulseops.publicstatus.web;

import com.mateja.pulseops.publicstatus.domain.ServiceStatus;

import java.util.UUID;

// One row on the public status page: a service and its derived status.
public record PublicServiceStatus(UUID serviceId, String name, ServiceStatus status) {
}
