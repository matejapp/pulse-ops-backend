package com.mateja.pulseops.monitoredservice.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

// The logical thing we watch ("Payments API"). It owns no check logic itself — HttpMonitors point
// at it. Uniqueness of the name is enforced by a case-insensitive UNIQUE INDEX in the V2 migration
// (ux_monitored_services_name on lower(name)), not by a column constraint here.
@Entity
@Table(name = "monitored_service")
public class MonitoredService {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID serviceId;
    @Column(name = "name")
    private String name;
    @Column(name = "description")
    private String description;
    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    // JPA's required no-arg constructor (protected: hidden from app code).
    protected MonitoredService() {}

    public MonitoredService(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
