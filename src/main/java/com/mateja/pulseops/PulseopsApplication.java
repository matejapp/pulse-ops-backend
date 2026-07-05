package com.mateja.pulseops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PulseopsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PulseopsApplication.class, args);
    }

}
