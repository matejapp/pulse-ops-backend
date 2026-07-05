package com.mateja.pulseops.checkresult.domain;

import com.mateja.pulseops.httpmonitor.domain.HttpMonitor;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "check_result")
public class CheckResult {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID checkResultId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_id", nullable = false, foreignKey = @ForeignKey(name = "fk_check_result_monitor"))
    private HttpMonitor monitor;
    @Column(nullable = false, name = "success")
    private boolean success;
    @Column(name = "response_status")
    private Integer responseStatus;
    @Column(nullable = false ,name = "latency_ms")
    private int latencyMs;
    @Column(name = "error_message")
    private String errorMessage;
    @Column(nullable = false, name = "checked_at")
    private Instant checkedAt;

    protected CheckResult(){}

    public CheckResult(HttpMonitor monitor, boolean success, Integer responseStatus, int latencyMs, String errorMessage, Instant checkedAt) {
        this.monitor = monitor;
        this.success = success;
        this.responseStatus = responseStatus;
        this.latencyMs = latencyMs;
        this.errorMessage = errorMessage;
        this.checkedAt = checkedAt;
    }

    public UUID getCheckResultId() {
        return checkResultId;
    }

    public HttpMonitor getMonitor() {
        return monitor;
    }

    public boolean getSuccess() {
        return success;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public int getLatencyMs() {
        return latencyMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }
}

