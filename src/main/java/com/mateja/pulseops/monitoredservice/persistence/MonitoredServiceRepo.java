package com.mateja.pulseops.monitoredservice.persistence;

import com.mateja.pulseops.monitoredservice.domain.MonitoredService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonitoredServiceRepo extends JpaRepository<MonitoredService, UUID> {

    public boolean existsByNameIgnoreCase(String name);


    Optional<MonitoredService> findByNameIgnoreCase(String name);

}
