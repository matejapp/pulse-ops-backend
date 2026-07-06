package com.mateja.pulseops.incident.web;

import com.mateja.pulseops.incident.application.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {
    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping()
    public ResponseEntity<IncidentResponse> addIncident(@Valid @RequestBody OpenIncidentRequest incidentRequest) {
        IncidentResponse incidentResponse = incidentService.openIncident(incidentRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(incidentResponse);
    }

    @PostMapping("/{incidentId}/updates")
    public ResponseEntity<IncidentDetailResponse> addIncidentUpdate(@PathVariable UUID incidentId, @Valid  @RequestBody AddIncidentUpdateRequest incidentUpdateRequest) {
        IncidentDetailResponse incidentDetailResponse = incidentService.addUpdate(incidentId, incidentUpdateRequest);

        return ResponseEntity.status(HttpStatus.OK).body(incidentDetailResponse);
    }

    @GetMapping()
    public ResponseEntity<List<IncidentResponse>> getIncidents() {
        List<IncidentResponse> incidentResponses = incidentService.getAll();

        return ResponseEntity.status(HttpStatus.OK).body(incidentResponses);
    }

    @GetMapping("/{incidentId}")
    public ResponseEntity<IncidentDetailResponse> getIncident(@PathVariable UUID incidentId) {
        IncidentDetailResponse incidentDetailResponse = incidentService.getOne(incidentId);

        return ResponseEntity.status(HttpStatus.OK).body(incidentDetailResponse);
    }
}
