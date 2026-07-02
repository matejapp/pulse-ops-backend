package com.mateja.pulseops.auth.application;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String msg){
        super(msg);
    }
}
