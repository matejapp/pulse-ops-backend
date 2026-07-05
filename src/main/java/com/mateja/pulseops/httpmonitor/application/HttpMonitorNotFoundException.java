package com.mateja.pulseops.httpmonitor.application;

// Thrown when a monitor doesn't exist, OR exists but doesn't belong to the service in the URL path.
// Both collapse to the same 404 on purpose: from the caller's view "GET/DELETE /api/services/{s}/monitors/{m}"
// simply has no such monitor under that service — we don't distinguish "wrong parent" from "absent".
public class HttpMonitorNotFoundException extends RuntimeException {
    public HttpMonitorNotFoundException(String message) {
        super(message);
    }
}
