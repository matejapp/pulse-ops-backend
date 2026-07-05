package com.mateja.pulseops.httpmonitor.domain;

import com.mateja.pulseops.monitoredservice.domain.MonitoredService;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;


import java.time.Instant;
import java.util.UUID;

// One concrete check ("GET https://.../health, expect 200") belonging to a MonitoredService.
// This is the check DEFINITION/config — NOT the ping results. The per-run history will be a
// separate entity in a later milestone; don't store "last status" as source of truth here.
@Entity
@Table(name = "http_monitor")
public class HttpMonitor {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID monitorId;

    // Many monitors -> one service. LAZY: the parent service is NOT loaded until getMonitoredService()
    // is called. Because open-in-view=false, only touch it inside a transaction, or you'll get a
    // LazyInitializationException. @JoinColumn puts the FK (service_id) on THIS table.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "service_id",
            foreignKey = @ForeignKey(name = "fk_http_monitor_service"),
            nullable = false
    )
    private MonitoredService  monitoredService;
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private MonitorStatus status;
    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures;
    @Column(name = "target_url", nullable = false)
    private String targetUrl;
    // Stored as text ("GET"/"HEAD"); the DB CHECK constraint also restricts to those values.
    @Column(name = "http_method", nullable = false)
    @Enumerated(EnumType.STRING)
    private HttpMethod httpMethod;
    @Column(name = "expected_status", nullable = false)
    private int expectedStatus;
    @Column(name = "enabled", nullable = false)
    private boolean enabled;
    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private Instant createdAt;

    protected  HttpMonitor() {
    }

    // enabled defaults to true HERE, in Java. The DB has DEFAULT true too, but JPA always writes
    // the field in its INSERT, so a Java boolean left as false would override the DB default — hence
    // we set it explicitly so new monitors are active by default.
    public HttpMonitor(MonitoredService monitoredService, String targetUrl,
                       HttpMethod httpMethod, int expectedStatus) {
        this.monitoredService = monitoredService;
        this.targetUrl = targetUrl;
        this.httpMethod = httpMethod;
        this.expectedStatus = expectedStatus;
        this.enabled = true;
        this.status = MonitorStatus.UNCHECKED;   // birth state — fixed, not caller-supplied
        this.consecutiveFailures = 0;            // explicit for symmetry (int already 0)
    }

    public UUID getMonitorId() {
        return monitorId;
    }

    public MonitoredService getMonitoredService() {
        return monitoredService;
    }

    public MonitorStatus getStatus() {
        return status;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public int getExpectedStatus() {
        return expectedStatus;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void recordSuccess(){
       consecutiveFailures = 0;
       status = MonitorStatus.OPERATIONAL;
    }

    public void recordFailure(){
        consecutiveFailures++;
        if(consecutiveFailures >= 3){
            status =  MonitorStatus.DEGRADED;
        }
    }
}
