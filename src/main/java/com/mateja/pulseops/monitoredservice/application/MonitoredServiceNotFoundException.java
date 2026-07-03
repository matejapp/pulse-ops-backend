package com.mateja.pulseops.monitoredservice.application;

public class MonitoredServiceNotFoundException extends RuntimeException{
    public MonitoredServiceNotFoundException(String message){
        super(message);
    }
}
