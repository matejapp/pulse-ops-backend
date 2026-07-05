package com.mateja.pulseops.httpmonitor.web;

import com.mateja.pulseops.httpmonitor.application.HttpMonitorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/services/{serviceId}/monitors")
public class HttpMonitorController {

    private final HttpMonitorService service;

    public HttpMonitorController(HttpMonitorService service) {
        this.service = service;
    }

    @GetMapping()
    public ResponseEntity<List<HttpMonitorResponse>> getHttpMonitors(@PathVariable UUID serviceId) {

        List<HttpMonitorResponse> monitors = service.getAllMonitors(serviceId);

        return ResponseEntity.status(HttpStatus.OK).body(monitors);
    }

    @PostMapping
    public ResponseEntity<HttpMonitorResponse> createHttpMonitor(@PathVariable UUID serviceId, @Valid @RequestBody CreateHttpMonitorRequest createHttpMonitorRequest) {
        HttpMonitorResponse monitor = service.createMonitor(serviceId, createHttpMonitorRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(monitor);
    }

    @DeleteMapping("/{monitorId}")
    public ResponseEntity<HttpMonitorResponse> deleteMonitor(@PathVariable UUID serviceId, @PathVariable UUID monitorId) {
        service.deleteMonitor(serviceId, monitorId);

        return ResponseEntity.noContent().build();
    }
}
