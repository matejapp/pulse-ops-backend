package com.mateja.pulseops.incident.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_update")
public class IncidentUpdate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID incidentUpdateId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", foreignKey = @ForeignKey(name = "fk_incident_update_incident") , nullable = false)
    private Incident incident;
    @Column(name="status", nullable=false)
    @Enumerated(EnumType.STRING)
    private IncidentStatus status;
    @Column(name="message", nullable=false)
    private String message;
    @Column(name="created_at", updatable=false)
    @CreationTimestamp
    private Instant createdAt;

    protected IncidentUpdate(){}

    public IncidentUpdate(Incident incident, IncidentStatus status, String message) {
        this.incident = incident;
        this.status = status;
        this.message = message;
    }

    public UUID getIncidentUpdateId() {
        return incidentUpdateId;
    }

    public Incident getIncident() {
        return incident;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
