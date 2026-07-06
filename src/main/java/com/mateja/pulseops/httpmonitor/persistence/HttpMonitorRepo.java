package com.mateja.pulseops.httpmonitor.persistence;


import com.mateja.pulseops.httpmonitor.domain.HttpMonitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HttpMonitorRepo extends JpaRepository<HttpMonitor, UUID> {

    //Nested-property traversal. Underscore tells Spring Data to descent into the monitoredService
    List<HttpMonitor> findByMonitoredService_ServiceId(UUID serviceId);

    List<HttpMonitor> findByEnabledTrue();

    // (serviceId, status) for every monitor in ONE query, for the public status aggregation.
    // m.monitoredService.serviceId dereferences to the FK column (no join / no LAZY load), so this
    // never touches the parent entity. Aliases must match the projection getters (getServiceId/getStatus).
    @Query("select m.monitoredService.serviceId as serviceId, m.status as status from HttpMonitor m")
    List<ServiceMonitorStatusView> findServiceMonitorStatuses();
}
