package com.mateja.pulseops.incident.domain;

public class InvalidIncidentTransitionException extends RuntimeException {
    public InvalidIncidentTransitionException(String s) {
        super(s);
    }
}
