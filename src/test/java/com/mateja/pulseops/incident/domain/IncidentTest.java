package com.mateja.pulseops.incident.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Pure domain logic: the INVESTIGATING -> MONITORING -> RESOLVED state machine enforced by
// transitionTo (forward-only + terminal lock). No Spring, no mocks, no DB — just new + assert.
// The MonitoredService parent is irrelevant to transitions, so we pass null.
class IncidentTest {

    private Incident newIncident() {
        return new Incident(null, "Payments API down");
    }

    @Test
    void freshIncidentStartsInvestigatingWithNoResolvedAt() {
        Incident incident = newIncident();

        assertEquals(IncidentStatus.INVESTIGATING, incident.getCurrentStatus());
        assertNull(incident.getResolvedAt());
    }

    @Test
    void forwardTransitionToMonitoringWorks() {
        Incident incident = newIncident();

        incident.transitionTo(IncidentStatus.MONITORING);

        assertEquals(IncidentStatus.MONITORING, incident.getCurrentStatus());
        assertNull(incident.getResolvedAt());
    }

    @Test
    void sameStatusUpdateIsAllowed() {
        Incident incident = newIncident();

        // Posting a progress note without changing state (INVESTIGATING -> INVESTIGATING) is legal.
        incident.transitionTo(IncidentStatus.INVESTIGATING);

        assertEquals(IncidentStatus.INVESTIGATING, incident.getCurrentStatus());
    }

    @Test
    void transitionToResolvedStampsResolvedAt() {
        Incident incident = newIncident();

        incident.transitionTo(IncidentStatus.RESOLVED);

        assertEquals(IncidentStatus.RESOLVED, incident.getCurrentStatus());
        assertNotNull(incident.getResolvedAt());
    }

    @Test
    void investigatingDirectlyToResolvedIsAllowed() {
        Incident incident = newIncident();

        // Skipping MONITORING is a forward move, so it's permitted.
        incident.transitionTo(IncidentStatus.RESOLVED);

        assertEquals(IncidentStatus.RESOLVED, incident.getCurrentStatus());
    }

    @Test
    void backwardTransitionThrows() {
        Incident incident = newIncident();
        incident.transitionTo(IncidentStatus.MONITORING);

        assertThrows(InvalidIncidentTransitionException.class,
                () -> incident.transitionTo(IncidentStatus.INVESTIGATING));

        // State is unchanged after the rejected transition.
        assertEquals(IncidentStatus.MONITORING, incident.getCurrentStatus());
    }

    @Test
    void resolvedIncidentIsLocked() {
        Incident incident = newIncident();
        incident.transitionTo(IncidentStatus.RESOLVED);

        // A resolved incident is terminal — even a same-status update is rejected.
        assertThrows(InvalidIncidentTransitionException.class,
                () -> incident.transitionTo(IncidentStatus.RESOLVED));
        assertThrows(InvalidIncidentTransitionException.class,
                () -> incident.transitionTo(IncidentStatus.MONITORING));
    }
}
