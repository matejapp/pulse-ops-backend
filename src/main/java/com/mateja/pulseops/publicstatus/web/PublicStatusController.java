package com.mateja.pulseops.publicstatus.web;

import com.mateja.pulseops.publicstatus.application.PublicStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// Unauthenticated status page API. /api/public/** is permitAll in SecurityConfig, so these
// endpoints are reachable without a token — the tests assert exactly that (200 with no auth).
@RestController
@RequestMapping("/api/public")
public class PublicStatusController {

    private final PublicStatusService publicStatusService;

    public PublicStatusController(PublicStatusService publicStatusService) {
        this.publicStatusService = publicStatusService;
    }

    @GetMapping("/status")
    public ResponseEntity<PublicStatusResponse> getStatus() {
        return ResponseEntity.ok(publicStatusService.getStatusPage());
    }

    @GetMapping("/incidents")
    public ResponseEntity<List<PublicIncidentResponse>> getActiveIncidents() {
        return ResponseEntity.ok(publicStatusService.getActiveIncidents());
    }

    @GetMapping("/incidents/{incidentId}")
    public ResponseEntity<PublicIncidentDetailResponse> getIncident(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(publicStatusService.getIncident(incidentId));
    }
}
