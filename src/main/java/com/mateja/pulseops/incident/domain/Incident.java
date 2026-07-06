package com.mateja.pulseops.incident.domain;

import com.mateja.pulseops.monitoredservice.domain.MonitoredService;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident")
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID incidentId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", foreignKey = @ForeignKey(name = "fk_incident_service"), nullable = false)
    private MonitoredService  service;
    @Column(name="title", nullable=false)
    private String title;
    @Column(name="current_status", nullable=false)
    @Enumerated(EnumType.STRING)
    private IncidentStatus currentStatus;
    @Column(name="created_at", updatable=false)
    @CreationTimestamp
    private Instant createdAt;
    @Column(name="resolved_at")
    private Instant resolvedAt;

    protected  Incident() {
    }

    public Incident(MonitoredService service, String title) {
        this.service = service;
        this.title = title;
        this.currentStatus = IncidentStatus.INVESTIGATING;
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    public MonitoredService getService() {
        return service;
    }

    public String getTitle() {
        return title;
    }

    public IncidentStatus getCurrentStatus() {
        return currentStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void transitionTo(IncidentStatus newStatus) {
        if(currentStatus == IncidentStatus.RESOLVED) {
            throw new InvalidIncidentTransitionException("Incident is Resolved");
        }

        if(newStatus.ordinal() < this.currentStatus.ordinal()) {
            throw new InvalidIncidentTransitionException(currentStatus + " -> " + newStatus);
        }

        currentStatus =  newStatus;
        if (newStatus == IncidentStatus.RESOLVED) {
            this.resolvedAt = Instant.now();
        }
    }
}
