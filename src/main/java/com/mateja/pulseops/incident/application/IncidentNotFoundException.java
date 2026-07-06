package com.mateja.pulseops.incident.application;

// Thrown when no incident exists for the given id. Mapped to 404 in GlobalExceptionHandler.
public class IncidentNotFoundException extends RuntimeException {
    public IncidentNotFoundException(String message) {
        super(message);
    }
}
