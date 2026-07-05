package com.mateja.pulseops.httpmonitor.persistence;


import com.mateja.pulseops.httpmonitor.domain.HttpMonitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HttpMonitorRepo extends JpaRepository<HttpMonitor, UUID> {

    //Nested-property traversal. Underscore tells Spring Data to descent into the monitoredService
    List<HttpMonitor> findByMonitoredService_ServiceId(UUID serviceId);


}
